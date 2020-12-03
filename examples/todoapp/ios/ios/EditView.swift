import SwiftUI
import Todo

struct EditView: View {
    private let component: TodoEdit
    
    @ObservedObject
    private var models: ObservableValue<TodoEditModel>
    
    init(_ component: TodoEdit) {
        self.component = component
        self.models = ObservableValue(component.models)
    }
    
    var body: some View {
        let model = models.value
        let binding = Binding(get: { model.text }, set: component.onTextChanged)
        
        return NavigationView {
            VStack{
                TextEditor(text: binding)
                    .frame(maxHeight: .infinity)
                    .padding(8)
                
                HStack {
                    Text("Completed")
                    Image(systemName: model.isDone ? "checkmark.square" : "square")
                        .onTapGesture { self.component.onDoneChanged(isDone: !model.isDone) }
                }.padding(8)
            }
            .navigationBarTitle("Edit todo", displayMode: .inline)
            .navigationBarItems(
                leading: Button(action: { withAnimation { component.onCloseClicked() } } ) {
                    HStack {
                        Image(systemName: "chevron.left")
                        Text("Close")
                    }
                }
            )
        }
    }
}

struct EditView_Previews: PreviewProvider {
    static var previews: some View {
        EditView(StubTodoEdit())
    }
    
    class StubTodoEdit: TodoEdit {
        let models: Value<TodoEditModel> =
            valueOf(
                TodoEditModel(
                    text: "Text",
                    isDone: true
                )
            )
        
        func onCloseClicked() {
        }
        
        func onDoneChanged(isDone: Bool) {
        }
        
        func onTextChanged(text: String) {
        }
    }
}
