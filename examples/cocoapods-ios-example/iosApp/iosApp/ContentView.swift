import UIKit
import SwiftUI
import shared


struct ContentView: View {
    var body: some View {
        ZStack {
            Color.white.ignoresSafeArea(.all) // status bar color
            ComposeView()
                .ignoresSafeArea(.all, edges: .bottom) // Compose has own keyboard handler
        }.preferredColorScheme(.light)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
