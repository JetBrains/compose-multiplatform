import UIKit
import SwiftUI
import benchmarks

struct ContentView: View {
    var body: some View {
        if (Main_iosKt.runReal()) {
            ComposeView()
                .ignoresSafeArea(.all) // Compose has own keyboard handler
        } else {
            VStack {
                Text("Hello, world!")
            }
            .padding()
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
