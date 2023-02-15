import SwiftUI

@main
struct iOSApp: App {

	@State private var textState: String = "text state"
	@State private var interactResult: InteractResult = InteractResult.init(Color.teal, false)
	@FocusState private var textFieldFocused: Bool

    let gradient = LinearGradient(colors: [Color(getCGColor(0xFF7F52FF)).opacity(1.0),
                                           Color(getCGColor(0xFFC811E2)).opacity(1.0),
                                           Color(getCGColor(0xFFE54857)).opacity(1.0)],
			startPoint: .topLeading,
			endPoint: .bottomTrailing
    )

	var body: some Scene {
		WindowGroup {
			TabView {
                NavigationView {
                    ZStack {
                        VStack {
                            gradient
                                .ignoresSafeArea(edges: .top)
                                .frame(height: 0)
                            Spacer()
                        }

                        VStack {
                            ComposeViewControllerToSwiftUI { result in
                                interactResult = result
                            }.position(x: UIScreen.main.bounds.width / 2, y: UIScreen.main.bounds.height / 2)

                            HStack {
                                TextField("Type message...", text: $textState, axis: .vertical)
                                        .focused($textFieldFocused)
                                        .lineLimit(3)
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
                            }
                                    .padding(10)
                                    .background(RoundedRectangle(cornerRadius: 10).fill(interactResult.swiftUIColor.opacity(0.7)))
                                    .padding(6)
                        }
                    }
                            .navigationBarTitleDisplayMode(.inline)
                            .navigationTitle("Compose inside SwiftUI")
//                            .toolbarBackground(gradient, for: .navigationBar)
//                            .toolbarBackground(.visible, for: .navigationBar)
                            .statusBar(hidden: false)
                }
						.tabItem {
							Label("Compose", systemImage: "square.and.pencil")
						}
						.toolbar(.visible, for: .tabBar)
						.toolbarBackground(gradient, for: .tabBar)
						.toolbarBackground(.visible, for: .tabBar)
                        .preferredColorScheme(.light)

				NavigationView {
					VStack {
						Text("SwiftUI screen")
					}
                        .navigationBarTitleDisplayMode(.inline)
                        .navigationTitle("SwiftUI")
				}
                    .tabItem { Label("SwiftUI", systemImage: "list.dash") }
                    .toolbar(.visible, for: .tabBar)
                    .toolbarBackground(Color.yellow, for: .tabBar)
                    .toolbarBackground(.visible, for: .tabBar)
                    .preferredColorScheme(interactResult.darkTheme ? .dark : .light)

			}
		}
	}
}
