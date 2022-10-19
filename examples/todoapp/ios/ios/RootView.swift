import SwiftUI
import Todo

struct RootView: View {
    @ObservedObject
    private var childStack: ObservableValue<ChildStack<AnyObject, TodoRootChild>>
    
    init(_ component: TodoRoot) {
        self.childStack = ObservableValue(component.childStack)
    }
    
    var body: some View {
        let child = self.childStack.value.active.instance
        
        switch child {
        case let main as TodoRootChild.Main:
            MainView(main.component)

        case let edit as TodoRootChild.Edit:
            EditView(edit.component)
                .transition(
                    .asymmetric(
                        insertion: AnyTransition.move(edge: .trailing),
                        removal: AnyTransition.move(edge: .trailing)
                    )
                )
                .animation(.easeInOut)
            
        default: EmptyView()
        }
    }
}

struct RootView_Previews: PreviewProvider {
    static var previews: some View {
        RootView(StubTodoRoot())
    }
    
    class StubTodoRoot : TodoRoot {
        let childStack: Value<ChildStack<AnyObject, TodoRootChild>> =
            simpleChildStack(.Main(component: MainView_Previews.StubTodoMain()))
    }
}
