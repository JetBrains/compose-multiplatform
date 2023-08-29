import UIKit
import SwiftUI
import shared
import MapKit

struct ComposeViewControllerInSwiftUI: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.ComposeOnly()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
