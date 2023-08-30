import UIKit
import SwiftUI
import shared
import MapKit

struct ComposeViewControllerInSwiftUI: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.ComposeEntryPoint()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
