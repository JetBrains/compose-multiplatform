import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            ComposeInSwiftUIScreen()
                .tabItem {
                    Label("Compose in SwiftUI", systemImage: "star.fill")
                }

            YetAnotherSwiftUIScreen()
                .tabItem {
                    Label("SwiftUI", systemImage: "gear")
                }
        }
    }
}
