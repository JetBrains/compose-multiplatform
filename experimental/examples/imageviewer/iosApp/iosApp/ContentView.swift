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
    @State var cameraScreen:Bool = false
    var body: some View {
        if(cameraScreen) {
            CameraScreen()
        } else {
            ComposeView(openCamera: { cameraScreen = true })
                    .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
        }
    }
}

struct CameraScreen: View {
    var body: some View {
        Text("Camera screen")//todo
    }
}

