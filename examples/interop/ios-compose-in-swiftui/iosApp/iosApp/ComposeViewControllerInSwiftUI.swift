import UIKit
import SwiftUI
import shared
import MapKit

struct ComposeLayer: View {
    var body: some View {
        ScreenTemplate(title: "Compose Multiplatform in SwiftUI") {
            ComposeViewControllerInSwiftUI()
                    .ignoresSafeArea(.keyboard) // Compose has its own keyboard handler
        }
    }
}

struct ComposeViewControllerInSwiftUI: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.ComposeEntryPoint()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
