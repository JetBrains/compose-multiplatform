import UIKit
import SwiftUI
import shared

struct ContentView: View {
    var body: some View {
        ZStack {
            Color(#colorLiteral(red: 0.235, green: 0.247, blue: 0.255, alpha: 1)).ignoresSafeArea(.all)
            ComposeView()
                .ignoresSafeArea(.all, edges: .bottom) // Compose has own keyboard handler
        }.preferredColorScheme(.dark)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
