import SwiftUI
import Todo

struct RootView: View {
    @ObservedObject
    private var routerStates: ObservableValue<RouterState<AnyObject, TodoRootChild>>
    
    init(_ component: TodoRoot) {
        self.routerStates = ObservableValue(component.routerState)
    }
    
    var body: some View {
        let child = self.routerStates.value.activeChild.instance
        
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
        let routerState: Value<RouterState<AnyObject, TodoRootChild>> =
            simpleRouterState(TodoRootChild.Main(component: MainView_Previews.StubTodoMain()))
    }
}
