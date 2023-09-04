import UIKit
import SwiftUI
import shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ZStack {
                Color.white.ignoresSafeArea(.all) // status bar color
                ComposeView()
                    .ignoresSafeArea(.all, edges: .bottom) // Compose has own keyboard handler
            }.preferredColorScheme(.light)
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
