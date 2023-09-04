import SwiftUI

struct ComposeInSwiftUIScreen: View {
    var body: some View {
        ZStack {
            ComposeLayer()
            TextInputLayer()
        }
            .onTapGesture {
                // Hide keyboard on tap outside of TextField
                UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
            }
    }
}

struct TextInputLayer: View {
    @State private var textState: String = "Example of SwiftUI view overlaying the Compose"
    @FocusState private var textFieldFocused: Bool

    var body: some View {
        VStack {
            Spacer()
            VStack {
                TextField("empty TextField", text: $textState, axis: .vertical)
                    .focused($textFieldFocused)
                    .lineLimit(3)
            }
                .padding(12)
                .background(RoundedRectangle(cornerRadius: 10).colorInvert().opacity(0.6))
                .padding(32)
        }
    }
}
