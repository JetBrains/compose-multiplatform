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
        GradientTemplate(title: "The Composers Chat") {
            ComposeViewControllerToSwiftUI()
                .ignoresSafeArea(.keyboard) // Compose have own keyboard handler
        }
    }
}

struct TextInputLayer: View {
    @State private var textState: String = ""
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
                        Image(systemName: "arrow.up.circle.fill")
                            .tint(Color(red: 0.671, green: 0.365, blue: 0.792))
                    }
                }
            }.padding(15).background(RoundedRectangle(cornerRadius: 200).fill(.white).opacity(0.95)).padding(15)
        }
    }
}
