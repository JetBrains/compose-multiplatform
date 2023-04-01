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
    private let openShareController: (Any) -> ()

    init(openShareController: @escaping (Any) -> ()) {
        self.openShareController = openShareController
    }

    func makeUIViewController(context: Context) -> UIViewController {
        let controller = Main_iosKt.MainViewController(openShareController: openShareController)
        controller.overrideUserInterfaceStyle = .light
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ZStack {
            ComposeView(openShareController: )
                    .ignoresSafeArea(.all) // Compose has own keyboard handler
            VStack {
                gradient.ignoresSafeArea(edges: .top).frame(height: 0)
                Spacer()
            }
        }.preferredColorScheme(.dark)
    }
}
