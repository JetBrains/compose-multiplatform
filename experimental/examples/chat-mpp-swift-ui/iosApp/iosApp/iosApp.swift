import SwiftUI

@main
struct iOSApp: App {

	@State private var textState: String = "text state"
	@State private var interactResult: InteractResult = InteractResult.init(Color.teal, false)
	@FocusState private var textFieldFocused: Bool

	let gradient = LinearGradient(colors: [.blue.opacity(1.0), .green.opacity(1.0)],
			startPoint: .topLeading,
			endPoint: .bottomTrailing)

	init() {

	}

	var body: some Scene {
		WindowGroup {
			TabView {

				ZStack {
					NavigationView {
						VStack {
							ComposeViewControllerToSwiftUI { result in
								interactResult = result
							}
									.position(x: UIScreen.main.bounds.width / 2, y: UIScreen.main.bounds.height / 2)

							HStack {
								TextField("Type message...", text: $textState, axis: .vertical)
										.focused($textFieldFocused)
										.lineLimit(3)
										.background(Color.pink.opacity(0.0), in: RoundedRectangle(cornerRadius: 10))
//                                    .textFieldStyle(.roundedBorder)
								if (textState.count > 0) {
									Button(action: {
										textFieldFocused = false
										textState = ""
										sendMessage(textState)
									}) {
										HStack {
											Image(systemName: "play.fill")
											Text("Send")
										}
									}
								}
							}.padding(10)
									.background(
											RoundedRectangle(cornerRadius: 10)
													.fill(interactResult.swiftUIColor.opacity(0.7))
									)
									.padding(6)
						}
								.navigationBarTitleDisplayMode(.inline)
								.navigationTitle("Compose inside SwiftUI")
								.toolbarBackground(interactResult.swiftUIColor, for: .navigationBar)
								.toolbarBackground(.visible, for: .navigationBar)
								.statusBar(hidden: false)
					}.frame(maxHeight: .infinity)

				}
						.tabItem {
							Label("Compose", systemImage: "square.and.pencil")
						}
						.toolbar(.visible, for: .tabBar)
						.toolbarBackground(Color.purple, for: .tabBar)
						.toolbarBackground(.visible, for: .tabBar)
						.preferredColorScheme(interactResult.darkTheme ? .dark : .light)


				NavigationView {
					VStack {
						Text("Page 2")
					}
							.navigationBarTitleDisplayMode(.inline)
							.navigationTitle("SwiftUI")
				}
						.tabItem {
							Label("SwiftUI", systemImage: "list.dash")
						}
						.toolbar(.visible, for: .tabBar)
						.toolbarBackground(Color.yellow, for: .tabBar)
						.toolbarBackground(.visible, for: .tabBar)
						.preferredColorScheme(interactResult.darkTheme ? .dark : .light)


			}
		}
	}
}
