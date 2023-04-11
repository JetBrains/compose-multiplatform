import SwiftUI

let gradient = LinearGradient(
        colors: gradient3Colors(),
        startPoint: .topLeading, endPoint: .bottomTrailing
)

@main
struct iOSApp: App {
	var body: some Scene {
		WindowGroup {
			TabView {
                ComposeInsideSwiftUIScreen()
                    .tabItem { Label("Compose", systemImage: "square.and.pencil") }

                YetAnotherSwiftUIScreen()
                    .tabItem { Label("SwiftUI", systemImage: "list.dash") }
                
            }.accentColor(.white).preferredColorScheme(.dark)
		}
	}
}
