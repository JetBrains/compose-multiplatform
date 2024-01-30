import UIKit
import SwiftUI
import shared
import MapKit

struct ComposeViewControllerRepresentable: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.ComposeEntryPointWithUIView(createUIView: { () -> UIView in
            SwiftUIInUIView(
                content: VStack {
                    Text("SwiftUI in Compose Multiplatform")
                }
            )
        })
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

private class SwiftUIInUIView<Content: View>: UIView {

    init(content: Content) {
        super.init(frame: CGRect())
        let hostingController = UIHostingController(rootView: content)
        hostingController.view.translatesAutoresizingMaskIntoConstraints = false
        addSubview(hostingController.view)
        NSLayoutConstraint.activate([
            hostingController.view.topAnchor.constraint(equalTo: topAnchor),
            hostingController.view.leadingAnchor.constraint(equalTo: leadingAnchor),
            hostingController.view.trailingAnchor.constraint(equalTo: trailingAnchor),
            hostingController.view.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
