import SwiftUI

@main
struct iOSApp: App {
	var body: some Scene {
		WindowGroup {
			TabView {
                ComposeInSwiftUIScreen()
                    .tabItem { Label("Compose in SwiftUI", systemImage: "star.fill") }

                YetAnotherSwiftUIScreen()
                    .tabItem { Label("SwiftUI", systemImage: "gear") }
                
            }
		}
	}
}
