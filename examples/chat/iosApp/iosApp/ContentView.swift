import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            ComposeInsideSwiftUIScreen()
                .tabItem {
                    Label("Group Chat", systemImage: "rectangle.3.group.bubble.left")
                }

            YetAnotherSwiftUIScreen()
                .tabItem {
                    Label("Settings", systemImage: "gear")
                }

        }
            .accentColor(Color(red: 0.671, green: 0.365, blue: 0.792)).preferredColorScheme(.light)
    }
}

let gradient = LinearGradient(
        colors: [
            Color(red: 0.933, green: 0.937, blue: 0.953),
            Color(red: 0.902, green: 0.941, blue: 0.949)
        ],
        startPoint: .topLeading, endPoint: .bottomTrailing
)
