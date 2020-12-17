import SwiftUI
import Todo

struct MainView: View {
    private let component: TodoMain
    
    @ObservedObject
    private var models: ObservableValue<TodoMainModel>
    
    init(_ component: TodoMain) {
        self.component = component
        self.models = ObservableValue(component.models)
    }
    
    var body: some View {
        let model = models.value
        
        return NavigationView {
            VStack {
                ListView(
                    items: model.items,
                    onItemClicked: component.onItemClicked,
                    onDoneChanged: component.onItemDoneChanged
                )

                InputView(
                    textBinding: Binding(get: { model.text }, set: component.onInputTextChanged),
                    onAddClicked: component.onAddItemClicked
                )
            }.navigationBarTitle("Todo Sample", displayMode: .inline)
        }
    }
}

struct MainView_Previews: PreviewProvider {
    static var previews: some View {
        MainView(StubTodoMain())
    }
    
    class StubTodoMain: TodoMain {
        let models: Value<TodoMainModel> =
            valueOf(
                TodoMainModel(
                    items: [
                        TodoItem(id: 1, order: 1, text: "Item 1", isDone: false),
                        TodoItem(id: 2, order: 2, text: "Item 2", isDone: true),
                        TodoItem(id: 3, order: 3, text: "Item 3", isDone: false)
                    ],
                    text: "Text"
                )
            )
        
        func onAddItemClicked() {
        }
        
        func onInputTextChanged(text: String) {
        }
        
        func onItemClicked(id: Int64) {
        }
        
        func onItemDeleteClicked(id: Int64) {
        }
        
        func onItemDoneChanged(id: Int64, isDone: Bool) {
        }
    }
}
