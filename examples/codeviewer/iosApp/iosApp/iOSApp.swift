import SwiftUI

@main
struct iOSApp: App {
	var body: some Scene {
		WindowGroup {
		    ZStack {
		        Color(#colorLiteral(red: 0.235, green: 0.247, blue: 0.255, alpha: 1)).ignoresSafeArea(.all)
			    ContentView()
			}.preferredColorScheme(.dark)
		}
	}
}