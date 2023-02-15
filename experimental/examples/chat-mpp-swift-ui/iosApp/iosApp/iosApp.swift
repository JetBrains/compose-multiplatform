import SwiftUI

@main
struct iOSApp: App {

	@State private var textState: String = "text message"
	@State private var interactResult: InteractResult = InteractResult.init(Color.teal, true)
	@FocusState private var textFieldFocused: Bool

    let gradient = LinearGradient(
            colors: gradient3Colors(),
			startPoint: .topLeading, endPoint: .bottomTrailing
    )

	var body: some Scene {
		WindowGroup {
			TabView {
                GradientTemplate(gradient: gradient, title: "Compose inside SwiftUI") {
                    VStack {
                        ComposeViewControllerToSwiftUI { result in
                            interactResult = result
                        }.position(x: UIScreen.main.bounds.width / 2, y: UIScreen.main.bounds.height / 2)
                        HStack {
                            TextField("Type message...", text: $textState, axis: .vertical)
                                    .focused($textFieldFocused)
                                    .lineLimit(3)
                            if (!textState.isEmpty) {
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
                        }.padding(10)
                                .background(RoundedRectangle(cornerRadius: 10).fill(gradient).opacity(0.8))
                                .padding(6)
                        Rectangle().fill(Color.clear).frame(height: 0).background(gradient)
                    }
                }.tabItem { Label("Compose", systemImage: "square.and.pencil") }

                GradientTemplate(gradient: gradient, title: "SwiftUI") {
                    Text("SwiftUI screen")
                }.tabItem { Label("SwiftUI", systemImage: "list.dash") }
            }
                    .accentColor(.white)
                    .preferredColorScheme(interactResult.darkTheme ? .dark : .light)
		}
	}
}

struct GradientTemplate<Content: View>: View {
    var gradient: LinearGradient
    var title: String
    var content: () -> Content

    var body: some View {
        NavigationView {
            ZStack {
                VStack {
                    gradient.ignoresSafeArea(edges: .top).frame(height: 0)
                    Spacer()
                }
                VStack {
                    content().frame(maxHeight: .infinity)
                    Rectangle().fill(Color.clear).frame(height: 0).background(gradient)
                }
            }
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .statusBar(hidden: false)
        }
            .toolbar(.visible, for: .tabBar)

    }
}
