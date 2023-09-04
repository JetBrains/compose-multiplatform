import SwiftUI
import shared
import UIKit

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeViewControllerToSwiftUI()
                .ignoresSafeArea(.all)
        }
    }
}

struct ComposeViewControllerToSwiftUI: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.ComposeEntryPoint()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
