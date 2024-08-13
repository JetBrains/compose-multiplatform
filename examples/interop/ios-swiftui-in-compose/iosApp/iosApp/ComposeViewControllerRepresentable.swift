import UIKit
import SwiftUI
import shared
import MapKit

struct ComposeViewControllerRepresentable: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.ComposeEntryPointWithUIViewController(createUIViewController: { () -> UIViewController in
            let swiftUIView = VStack {
                Text("SwiftUI in Compose Multiplatform")
            }
            return UIHostingController(rootView: swiftUIView)
        })
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
