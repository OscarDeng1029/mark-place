import SwiftUI
import UIKit
import AtlasShared

@main
struct MyLittleAtlasApp: App {
    private let services = IosNativeServices()

    var body: some Scene {
        WindowGroup {
            AtlasRoot(services: services)
                .ignoresSafeArea()
                .preferredColorScheme(.light)
        }
    }
}

private struct AtlasRoot: UIViewControllerRepresentable {
    let services: IosNativeServices

    func makeUIViewController(context: Context) -> UIViewController {
        IosAppKt.MainViewController(bridge: services)
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}
