import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    private let openCamera: () -> ()
    init(openCamera: @escaping () -> ()) {
        self.openCamera = openCamera
    }

    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController(openCamera: self.openCamera)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView(openCamera: { print("open camera in SwiftUI") })
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}



