import SwiftUI
import Todo

struct ContentView: View {
    @State
    private var componentHolder =
        ComponentHolder {
            TodoRootComponent(
                componentContext: $0,
                storeFactory: DefaultStoreFactory(),
                database: TodoDatabaseCompanion().invoke(driver: TodoDatabaseDriverFactoryKt.TodoDatabaseDriver())
            )
        }
    
    var body: some View {
        RootView(componentHolder.component)
            .onAppear { LifecycleRegistryExtKt.resume(self.componentHolder.lifecycle) }
            .onDisappear { LifecycleRegistryExtKt.stop(self.componentHolder.lifecycle) }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
