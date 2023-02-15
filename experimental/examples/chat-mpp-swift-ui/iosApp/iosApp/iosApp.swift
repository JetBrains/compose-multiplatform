import SwiftUI

@main
struct iOSApp: App {

	@State private var textState: String = "text message"
	@State private var interactResult: InteractResult = InteractResult.init(Color.teal, true)
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
                            gradient.ignoresSafeArea(edges: .top).frame(height: 0)
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
                                        sendMessage(textState)
                                        textFieldFocused = false
                                        textState = ""
                                    }) {
                                        HStack {
                                            Image(systemName: "play.fill")
                                            Text("Send")
                                        }.tint(.white)
                                    }
                                }
                            }
                                .padding(10)
                                .background(RoundedRectangle(cornerRadius: 10).fill(gradient).opacity(0.8))
                                .padding(6)
                            Rectangle().fill(Color.clear).frame(height: 0).background(gradient)
                        }
                    }
                        .navigationBarTitleDisplayMode(.inline)
                        .navigationTitle("Compose inside SwiftUI")
                        .statusBar(hidden: false)
                }
                    .tabItem { Label("Compose", systemImage: "square.and.pencil") }
                    .toolbar(.visible, for: .tabBar)
                    .preferredColorScheme(interactResult.darkTheme ? .dark : .light)

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
