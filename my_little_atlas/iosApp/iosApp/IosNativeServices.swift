import Foundation
import UIKit
import Photos
import PhotosUI
import CoreLocation
import UniformTypeIdentifiers
import AtlasShared

/// UIKit owns system sheets and permissions; Compose owns every app page.
final class IosNativeServices: NSObject, IosPlatformBridge, CLLocationManagerDelegate {
    private let root: URL
    private let defaults = UserDefaults.standard
    private var photoDelegate: PhotoSelection?
    private var documentDelegate: BackupSelection?
    private var locationManager: CLLocationManager?
    private var locationResult: ((String?, String?, String?) -> KotlinUnit)?
    private var locationTimeout: DispatchWorkItem?
    private var requests: [String: URLSessionDataTask] = [:]
    private var incomingText: ((String) -> KotlinUnit)?
    private var pendingText: String?
    private lazy var session: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.urlCache = URLCache(memoryCapacity: 24 * 1024 * 1024, diskCapacity: 64 * 1024 * 1024)
        configuration.requestCachePolicy = .useProtocolCachePolicy
        configuration.httpMaximumConnectionsPerHost = 6
        return URLSession(configuration: configuration)
    }()

    override init() {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        #if DEBUG
        let uiTest = ProcessInfo.processInfo.environment["ATLAS_UI_TEST_DEMO"] == "1"
        root = support.appendingPathComponent(uiTest ? "AtlasUITests" : "MyLittleAtlas", isDirectory: true)
        #else
        root = support.appendingPathComponent("MyLittleAtlas", isDirectory: true)
        #endif
        super.init()
        try? FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        #if DEBUG
        if uiTest {
            try? Data(IosAppKt.previewAtlasJson().utf8).write(to: dataURL, options: .atomic)
        }
        #endif
    }

    private var dataURL: URL { root.appendingPathComponent("atlas.json") }
    var serviceUrl: String {
        get { defaults.string(forKey: "atlas.service.url") ?? "http://127.0.0.1:3212" }
        set { defaults.set(newValue.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/")), forKey: "atlas.service.url") }
    }
    func readData() -> String? {
        guard FileManager.default.fileExists(atPath: dataURL.path) else { return nil }
        // An unreadable existing file must reach the repository's recovery path, never become a new empty library.
        return (try? String(contentsOf: dataURL, encoding: .utf8)) ?? "<unreadable atlas data>"
    }
    func writeData(content: String) -> String? {
        do {
            try Data(content.utf8).write(to: dataURL, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            return nil
        } catch { return "无法保存记录，请检查设备空间。原记录已保留。" }
    }
    func today() -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }
    func uniqueId() -> String { UUID().uuidString.lowercased() }
    func nowMillis() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }
    func readImage(path: String) -> KotlinByteArray? {
        let url = path.hasPrefix("/") ? URL(fileURLWithPath: path) : root.appendingPathComponent(path)
        guard let data = try? Data(contentsOf: url), data.count <= 30_000_000 else { return nil }
        return data.kotlinBytes()
    }

    func pickPhoto(onResult: @escaping (String?) -> KotlinUnit) {
        var configuration = PHPickerConfiguration(photoLibrary: .shared())
        configuration.filter = .images
        configuration.selectionLimit = 1
        let picker = PHPickerViewController(configuration: configuration)
        let selection = PhotoSelection(root: root) { [weak self] path in
            self?.photoDelegate = nil
            _ = onResult(path)
        }
        photoDelegate = selection
        picker.delegate = selection
        picker.presentationController?.delegate = selection
        present(picker)
    }
    func exportBackup(json: String) {
        do {
            let folder = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
            try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
            let url = folder.appendingPathComponent("my-little-atlas-backup.json")
            try Data(json.utf8).write(to: url, options: .atomic)
            present(UIDocumentPickerViewController(forExporting: [url], asCopy: true))
        } catch { showError("备份导出失败，请重试。") }
    }
    func importBackup(onResult: @escaping (String?) -> KotlinUnit) {
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.json, .plainText], asCopy: true)
        picker.allowsMultipleSelection = false
        let selection = BackupSelection { [weak self] text in
            self?.documentDelegate = nil
            _ = onResult(text)
        }
        documentDelegate = selection
        picker.delegate = selection
        picker.presentationController?.delegate = selection
        present(picker)
    }
    func shareText(text: String) { share([text]) }
    func sharePng(bytes: KotlinByteArray) {
        guard let image = UIImage(data: bytes.swiftData()) else { showError("图片生成失败，请重试。"); return }
        share([image])
    }
    func savePng(bytes: KotlinByteArray, onResult: @escaping (KotlinBoolean) -> KotlinUnit) {
        guard let image = UIImage(data: bytes.swiftData()) else { _ = onResult(KotlinBoolean(bool: false)); return }
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else {
                DispatchQueue.main.async { _ = onResult(KotlinBoolean(bool: false)) }
                return
            }
            PHPhotoLibrary.shared().performChanges({
                PHAssetChangeRequest.creationRequestForAsset(from: image)
            }) { success, _ in
                DispatchQueue.main.async { _ = onResult(KotlinBoolean(bool: success)) }
            }
        }
    }

    func locate(onResult: @escaping (String?, String?, String?) -> KotlinUnit) {
        guard locationResult == nil else { _ = onResult(nil, nil, "正在获取位置，请稍候。"); return }
        guard CLLocationManager.locationServicesEnabled() else {
            _ = onResult(nil, nil, "系统定位服务未开启，也可以继续手动浏览。"); return
        }
        locationResult = onResult
        let manager = CLLocationManager()
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.delegate = self
        locationManager = manager
        let timeout = DispatchWorkItem { [weak self] in self?.finishLocation(nil, "暂时没有获得位置，请稍后重试。") }
        locationTimeout = timeout
        DispatchQueue.main.asyncAfter(deadline: .now() + 20, execute: timeout)
        requestLocationIfAuthorized(manager)
    }
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) { requestLocationIfAuthorized(manager) }
    private func requestLocationIfAuthorized(_ manager: CLLocationManager) {
        guard locationResult != nil else { return }
        switch manager.authorizationStatus {
        case .notDetermined: manager.requestWhenInUseAuthorization()
        case .authorizedAlways, .authorizedWhenInUse: manager.requestLocation()
        case .denied, .restricted: finishLocation(nil, "未获得定位权限。可在系统设置中开启，也可以继续手动浏览。")
        @unknown default: finishLocation(nil, "暂时无法获取位置。")
        }
    }
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        finishLocation(locations.last, locations.isEmpty ? "暂时没有获得位置，请重试。" : nil)
    }
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        finishLocation(nil, "定位暂时不可用，请稍后重试，也可以继续手动浏览。")
    }
    private func finishLocation(_ location: CLLocation?, _ error: String?) {
        locationTimeout?.cancel()
        locationTimeout = nil
        locationManager?.stopUpdatingLocation()
        locationManager?.delegate = nil
        locationManager = nil
        let callback = locationResult
        locationResult = nil
        _ = callback?(location.map { String($0.coordinate.latitude) }, location.map { String($0.coordinate.longitude) }, error)
    }

    func navigate(name: String, address: String, city: String, latitude: String?, longitude: String?) {
        let query = "\(name) \(address) \(city)"
        func url(_ base: String, _ items: [URLQueryItem]) -> URL? {
            var components = URLComponents(string: base)
            components?.queryItems = items
            return components?.url
        }
        let amap: URL?
        if let lat = latitude, let lon = longitude {
            amap = url("https://uri.amap.com/marker", [
                URLQueryItem(name: "position", value: "\(lon),\(lat)"), URLQueryItem(name: "name", value: name),
                URLQueryItem(name: "coordinate", value: "gaode"), URLQueryItem(name: "callnative", value: "1")
            ])
        } else {
            amap = url("https://uri.amap.com/search", [URLQueryItem(name: "keyword", value: query), URLQueryItem(name: "city", value: city)])
        }
        let apple = url("https://maps.apple.com/", [URLQueryItem(name: "q", value: query)])
        let sheet = UIAlertController(title: "选择导航方式", message: nil, preferredStyle: .actionSheet)
        for (title, destination) in [("高德地图", amap), ("Apple 地图", apple)] {
            if let destination {
                sheet.addAction(UIAlertAction(title: title, style: .default) { _ in UIApplication.shared.open(destination) })
            }
        }
        sheet.addAction(UIAlertAction(title: "取消", style: .cancel))
        present(sheet)
    }
    func openSource(url: String) {
        guard let target = URL(string: url), ["https", "http"].contains(target.scheme?.lowercased() ?? "") else { return }
        UIApplication.shared.open(target)
    }

    func request(url: String, jsonBody: String?, tile: Bool,
                 onResult: @escaping (KotlinByteArray?, String?) -> KotlinUnit) -> String {
        let id = UUID().uuidString
        guard let destination = URL(string: url),
              destination.scheme == "https" || (destination.scheme == "http" && ["127.0.0.1", "localhost"].contains(destination.host ?? "")) else {
            DispatchQueue.main.async { _ = onResult(nil, "服务地址需为 HTTPS，或本机调试地址。") }
            return id
        }
        var request = URLRequest(url: destination)
        request.timeoutInterval = tile ? 9 : 240
        request.setValue("MyLittleAtlas/0.1 (iOS native app)", forHTTPHeaderField: "User-Agent")
        request.setValue(tile ? "image/*" : "application/json", forHTTPHeaderField: "Accept")
        if let jsonBody {
            request.httpMethod = "POST"
            request.httpBody = Data(jsonBody.utf8)
            request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        }
        let task = session.dataTask(with: request) { [weak self] data, response, error in
            let http = response as? HTTPURLResponse
            var message: String?
            if let error {
                message = (error as NSError).code == NSURLErrorTimedOut ? "连接超时，请稍后重试。" : "无法连接，请检查网络及解析服务地址。"
            } else if !(200...299).contains(http?.statusCode ?? 0) {
                let json = data.flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] }
                message = json?["error"] as? String ?? "服务返回错误（\(http?.statusCode ?? 0)），请重试。"
            } else if (data?.count ?? 0) > (tile ? 1_000_000 : 2_500_000) {
                message = "服务返回的数据过大，请重试。"
            } else if tile && !(http?.mimeType?.hasPrefix("image/") ?? false) {
                message = "地图图片暂时不可用。"
            }
            let result = message == nil ? data?.kotlinBytes() : nil
            DispatchQueue.main.async {
                self?.requests.removeValue(forKey: id)
                _ = onResult(result, message)
            }
        }
        requests[id] = task
        task.resume()
        return id
    }
    func cancelRequest(id: String) {
        DispatchQueue.main.async { [weak self] in self?.requests.removeValue(forKey: id)?.cancel() }
    }

    func observeIncomingText(onText: @escaping (String) -> KotlinUnit) {
        incomingText = onText
        if let pendingText { self.pendingText = nil; _ = onText(pendingText) }
        consumeSharedImport()
    }
    func receiveImportURL(_ url: URL) {
        guard url.scheme == "mylittleatlas", url.host == "import",
              let text = URLComponents(url: url, resolvingAgainstBaseURL: false)?
                .queryItems?.first(where: { $0.name == "text" })?.value else { return }
        receiveText(text)
    }
    func consumeSharedImport() {
        guard let folder = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: "group.com.mylittleatlas.app") else { return }
        let file = folder.appendingPathComponent("pending-import.txt")
        guard let text = try? String(contentsOf: file, encoding: .utf8) else { return }
        // Consume only after the Compose host is ready, and leave no copy in the shared container.
        guard incomingText != nil else { return }
        try? FileManager.default.removeItem(at: file)
        receiveText(text)
    }
    private func receiveText(_ value: String) {
        let text = String(value.trimmingCharacters(in: .whitespacesAndNewlines).prefix(30_000))
        guard !text.isEmpty else { return }
        if let incomingText { _ = incomingText(text) } else { pendingText = text }
    }

    private func share(_ items: [Any]) { present(UIActivityViewController(activityItems: items, applicationActivities: nil)) }
    private func showError(_ message: String) {
        let alert = UIAlertController(title: "My Little Atlas", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "知道了", style: .default))
        present(alert)
    }
    private func present(_ controller: UIViewController) {
        guard var host = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows).first(where: \.isKeyWindow)?.rootViewController else { return }
        while let shown = host.presentedViewController, !shown.isBeingDismissed { host = shown }
        if let popover = controller.popoverPresentationController {
            popover.sourceView = host.view
            popover.sourceRect = CGRect(x: host.view.bounds.midX, y: host.view.bounds.midY, width: 1, height: 1)
            popover.permittedArrowDirections = []
        }
        host.present(controller, animated: true)
    }
}

private final class PhotoSelection: NSObject, PHPickerViewControllerDelegate, UIAdaptivePresentationControllerDelegate {
    private let root: URL
    private var completion: ((String?) -> Void)?
    init(root: URL, completion: @escaping (String?) -> Void) { self.root = root; self.completion = completion }
    private func finish(_ path: String?) {
        let callback = completion
        completion = nil
        callback?(path)
    }
    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) { finish(nil) }
    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)
        guard let item = results.first?.itemProvider, item.canLoadObject(ofClass: UIImage.self) else { finish(nil); return }
        item.loadObject(ofClass: UIImage.self) { [self] object, _ in
            guard let image = object as? UIImage else { DispatchQueue.main.async { self.finish(nil) }; return }
            let scale = min(1, 2400 / max(image.size.width, image.size.height))
            let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)
            let format = UIGraphicsImageRendererFormat()
            format.scale = 1
            let normalized = UIGraphicsImageRenderer(size: size, format: format).image { _ in image.draw(in: CGRect(origin: .zero, size: size)) }
            let path = "photos/\(UUID().uuidString.lowercased()).jpg"
            let target = root.appendingPathComponent(path)
            let saved: String?
            do {
                try FileManager.default.createDirectory(at: target.deletingLastPathComponent(), withIntermediateDirectories: true)
                guard let data = normalized.jpegData(compressionQuality: 0.9) else { throw CocoaError(.fileWriteUnknown) }
                try data.write(to: target, options: .atomic)
                saved = path
            } catch { saved = nil }
            DispatchQueue.main.async { self.finish(saved) }
        }
    }
}

private final class BackupSelection: NSObject, UIDocumentPickerDelegate, UIAdaptivePresentationControllerDelegate {
    private var completion: ((String?) -> Void)?
    init(completion: @escaping (String?) -> Void) { self.completion = completion }
    private func finish(_ text: String?) { let callback = completion; completion = nil; callback?(text) }
    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) { finish(nil) }
    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) { finish(nil) }
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard let url = urls.first else { finish(nil); return }
        let access = url.startAccessingSecurityScopedResource()
        defer { if access { url.stopAccessingSecurityScopedResource() } }
        guard let data = try? Data(contentsOf: url), data.count <= 20_000_000,
              let text = String(data: data, encoding: .utf8) else { finish(nil); return }
        finish(text)
    }
}

private extension Data {
    func kotlinBytes() -> KotlinByteArray {
        let bytes = KotlinByteArray(size: Int32(count))
        for (index, value) in enumerated() { bytes.set(index: Int32(index), value: Int8(bitPattern: value)) }
        return bytes
    }
}
private extension KotlinByteArray {
    func swiftData() -> Data {
        Data((0..<Int(size)).map { UInt8(bitPattern: get(index: Int32($0))) })
    }
}
