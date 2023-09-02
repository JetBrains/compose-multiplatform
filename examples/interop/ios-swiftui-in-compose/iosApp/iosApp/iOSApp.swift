import SwiftUI
import shared
import UIKit

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeViewControllerRepresentable()
                .ignoresSafeArea(.all)
        }
    }
}
