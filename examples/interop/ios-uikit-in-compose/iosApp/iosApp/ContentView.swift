import SwiftUI
import shared
import UIKit

struct ContentView: View {
    var body: some View {
        ComposeViewControllerToSwiftUI()
            .ignoresSafeArea(.all)
    }
}

struct ComposeViewControllerToSwiftUI: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.ComposeEntryPoint()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
