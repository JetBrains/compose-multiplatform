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
        Main_iosKt.setGlobalFromArgs(args: ProcessInfo.processInfo.arguments)
        if (!Main_iosKt.runReal()) {
            Main_iosKt.runBenchmarks()
        }
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}