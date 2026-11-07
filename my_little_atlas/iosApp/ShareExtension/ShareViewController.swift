import UIKit
import UniformTypeIdentifiers

/// iOS share sheets cannot force-open the containing app. Keep the text pending until its next foreground.
final class ShareViewController: UIViewController {
    private let preview = UITextView()
    private let saveButton = UIButton(type: .system)
    private let status = UILabel()
    private var text = ""

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 0.969, green: 0.949, blue: 0.914, alpha: 1)
        let title = UILabel()
        title.text = "收进私藏去处"
        title.font = .systemFont(ofSize: 24, weight: .semibold)
        let hint = UILabel()
        hint.text = "打开 App 后确认地点，确认后才会加入收藏。"
        hint.font = .systemFont(ofSize: 14)
        hint.textColor = .secondaryLabel
        hint.numberOfLines = 0
        preview.isEditable = false
        preview.backgroundColor = .white
        preview.layer.cornerRadius = 14
        preview.font = .systemFont(ofSize: 15)
        preview.textContainerInset = UIEdgeInsets(top: 14, left: 12, bottom: 14, right: 12)
        saveButton.setTitle("保存待导入内容", for: .normal)
        saveButton.titleLabel?.font = .systemFont(ofSize: 17, weight: .semibold)
        saveButton.backgroundColor = UIColor(red: 0.325, green: 0.388, blue: 0.306, alpha: 1)
        saveButton.setTitleColor(.white, for: .normal)
        saveButton.layer.cornerRadius = 14
        saveButton.isEnabled = false
        saveButton.addTarget(self, action: #selector(save), for: .touchUpInside)
        let cancel = UIButton(type: .system)
        cancel.setTitle("取消", for: .normal)
        cancel.addTarget(self, action: #selector(close), for: .touchUpInside)
        status.numberOfLines = 0
        status.font = .systemFont(ofSize: 13)
        status.textColor = .secondaryLabel
        let stack = UIStackView(arrangedSubviews: [title, hint, preview, status, saveButton, cancel])
        stack.axis = .vertical
        stack.spacing = 16
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 24),
            stack.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -24),
            stack.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 24),
            stack.bottomAnchor.constraint(lessThanOrEqualTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            preview.heightAnchor.constraint(equalToConstant: 200),
            saveButton.heightAnchor.constraint(equalToConstant: 50)
        ])
        loadContent()
    }
    private func loadContent() {
        let items = extensionContext?.inputItems as? [NSExtensionItem] ?? []
        let providers = items.flatMap { $0.attachments ?? [] }
        var pieces = items.compactMap { $0.attributedContentText?.string }
        let group = DispatchGroup()
        for provider in providers {
            let type: String
            if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) { type = UTType.url.identifier }
            else if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) { type = UTType.plainText.identifier }
            else { continue }
            group.enter()
            provider.loadItem(forTypeIdentifier: type, options: nil) { item, _ in
                let value = (item as? URL)?.absoluteString ?? (item as? String)
                DispatchQueue.main.async {
                    if let value { pieces.append(value) }
                    group.leave()
                }
            }
        }
        group.notify(queue: .main) { [weak self] in
            guard let self else { return }
            self.text = String(pieces.joined(separator: "\n").prefix(30_000))
            self.preview.text = self.text
            self.saveButton.isEnabled = !self.text.isEmpty
            self.status.text = self.text.isEmpty ? "未收到文字或链接。请复制分享内容，在 App 中粘贴导入。" : nil
        }
    }
    @objc private func save() {
        guard let folder = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: "group.com.mylittleatlas.app") else {
            status.text = "共享暂时不可用。请复制内容后，在私藏去处中粘贴导入。"
            return
        }
        do {
            try Data(text.utf8).write(to: folder.appendingPathComponent("pending-import.txt"), options: .atomic)
            extensionContext?.completeRequest(returningItems: nil)
        } catch { status.text = "未能保存，请重试或复制内容后在 App 中导入。" }
    }
    @objc private func close() {
        extensionContext?.cancelRequest(withError: NSError(domain: NSCocoaErrorDomain, code: NSUserCancelledError))
    }
}
