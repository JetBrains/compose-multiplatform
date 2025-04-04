import SwiftUI
import benchmarks
import Combine

class MemoryWarningMonitor: ObservableObject {
    private var cancellable: AnyCancellable?

    init() {
        self.cancellable = NotificationCenter.default
            .publisher(for: UIApplication.didReceiveMemoryWarningNotification)
            .sink { _ in
                self.didReceiveMemoryWarning()
            }
    }

    private func didReceiveMemoryWarning() {
//         print("Memory warning received! Cleaning resources.")
        RunGC_nativeKt.runGC()
    }
}

@main
struct iOSApp: App {
	init() {
	    MemoryWarningMonitor()
		Main_iosKt.main(args: ProcessInfo.processInfo.arguments)
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}