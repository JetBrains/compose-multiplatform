import SwiftUI

let gradient = LinearGradient(
        colors: [
            Color(red: 0.933, green: 0.937, blue: 0.953),
            Color(red: 0.902, green: 0.941, blue: 0.949)
        ],
        startPoint: .topLeading, endPoint: .bottomTrailing
)

@main
struct iOSApp: App {
	var body: some Scene {
		WindowGroup {
			TabView {
                ComposeInsideSwiftUIScreen()
                    .tabItem { Label("Compose in SwiftUI", systemImage: "star.fill") }

                YetAnotherSwiftUIScreen()
                    .tabItem { Label("SwiftUI", systemImage: "gear") }
                
            }
		}
	}
}
