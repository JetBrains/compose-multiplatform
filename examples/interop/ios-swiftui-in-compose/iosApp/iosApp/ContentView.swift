import SwiftUI
import shared
import UIKit

struct ContentView: View {
    var body: some View {
        ComposeViewControllerRepresentable()
            .ignoresSafeArea(.all)
    }
}
