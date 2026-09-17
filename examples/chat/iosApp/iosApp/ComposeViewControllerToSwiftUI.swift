import UIKit
import SwiftUI
import shared

struct ComposeViewControllerToSwiftUI: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.ChatViewController(
            extraTopInset: Double(ChatHeaderLayer.extraTopInset),
            extraBottomInset: 0
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
