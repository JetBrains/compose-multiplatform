import UIKit
import SwiftUI
import shared
import MapKit

struct ComposeViewControllerRepresentable: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.ComposeEntryPointWithUIView(createUIViewController: { () -> UIViewController in
            let hostingController = UIHostingController(
                rootView:
                    VStack {
                        Text("SwiftUI in Compose Multiplatform")
                    }
            )
            return hostingController
        })
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
