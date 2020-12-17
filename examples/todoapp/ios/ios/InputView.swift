import SwiftUI
import Todo

struct InputView: View {
    var textBinding: Binding<String>
    var onAddClicked: () -> Void
        
    var body: some View {
        HStack {
            TextField("Todo text", text: self.textBinding)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .edgesIgnoringSafeArea(Edge.Set.bottom)

            Button(action: self.onAddClicked) {
                Image(systemName: "plus")
            }.frame(minWidth: 36, minHeight: 36)
        }.padding(8)
    }
}

struct InputView_Previews: PreviewProvider {
    static var previews: some View {
        InputView(
            textBinding: Binding(get: { "Text" }, set: {_ in }),
            onAddClicked: {}
        )
    }
}
