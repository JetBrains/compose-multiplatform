import SwiftUI
import benchmarks

@main
struct iOSApp: App {
	init() {
		Main_iosKt.main()
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}