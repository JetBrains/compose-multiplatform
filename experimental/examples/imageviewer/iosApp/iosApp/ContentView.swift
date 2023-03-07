import UIKit
import SwiftUI
import shared

let gradient = LinearGradient(
        colors: [
            Color.black.opacity(0.6),
            Color.black.opacity(0.6),
            Color.black.opacity(0.5),
            Color.black.opacity(0.3),
            Color.black.opacity(0.0),
        ],
        startPoint: .top, endPoint: .bottom
)

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ZStack {
            ComposeView()
                    .ignoresSafeArea(.all) // Compose has own keyboard handler
            VStack {
                gradient.ignoresSafeArea(edges: .top).frame(height: 0)
                Spacer()
            }
        }.preferredColorScheme(.dark)
    }
}
