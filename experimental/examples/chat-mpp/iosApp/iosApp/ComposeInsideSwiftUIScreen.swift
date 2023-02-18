import SwiftUI

struct ComposeInsideSwiftUIScreen: View {
    var body: some View {
        ZStack {
            ComposeLayer()
            TextInputLayer()
        }.onTapGesture {
            // Hide keyboard on tap outside of TextField
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        }
    }
}

struct ComposeLayer: View {
    var body: some View {
        GradientTemplate(title: "Compose inside SwiftUI") {
            ComposeViewControllerToSwiftUI()
                .ignoresSafeArea(.keyboard) // Compose have own keyboard handler
        }
    }
}

struct TextInputLayer: View {
    @State private var textState: String = "text message"
    @FocusState private var textFieldFocused: Bool

    var body: some View {
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
    }
}
