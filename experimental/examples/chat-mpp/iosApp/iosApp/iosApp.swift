import SwiftUI

let gradient = LinearGradient(
        colors: gradient3Colors(),
        startPoint: .topLeading, endPoint: .bottomTrailing
)

@main
struct iOSApp: App {
	var body: some Scene {
		WindowGroup {
			TabView {
                GradientTemplate(title: "Compose inside SwiftUI") {
                    ComposeScreen()
                }.tabItem { Label("Compose", systemImage: "square.and.pencil") }
                GradientTemplate(title: "SwiftUI") {
                    SwiftUIScreen()
                }.tabItem { Label("SwiftUI", systemImage: "list.dash") }
            }.accentColor(.white).preferredColorScheme(.dark)
		}
	}
}

struct ComposeScreen: View {
    @State private var textState: String = "text message"
    @FocusState private var textFieldFocused: Bool
    var body: some View {
        ZStack {
            ComposeViewControllerToSwiftUI().ignoresSafeArea(.keyboard)
            VStack {
                Spacer()
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
                }.padding(10).background(RoundedRectangle(cornerRadius: 10).fill(gradient).opacity(0.8)).padding(6)
            }
            Rectangle().fill(Color.clear).frame(height: 0).background(gradient)
        }.onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        }
    }
}

struct SwiftUIScreen: View {
    var body: some View {
        VStack {
            Text("SwiftUI screen")
        }
    }
}

struct GradientTemplate<Content: View>: View {
    var title: String
    var content: () -> Content

    var body: some View {
        NavigationView {
            ZStack {
                surfaceColor()
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
