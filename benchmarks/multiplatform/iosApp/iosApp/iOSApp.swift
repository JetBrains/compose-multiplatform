import SwiftUI
import benchmarks

@main
struct iOSApp: App {
	init() {
		Main_iosKt.main(args: ProcessInfo.processInfo.arguments)
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}