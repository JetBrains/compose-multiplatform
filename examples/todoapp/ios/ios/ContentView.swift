import SwiftUI
import Todo

struct ContentView: View {
    @State
    private var componentHolder =
        ComponentHolder {
            TodoRootKt.TodoRoot(
                componentContext: $0,
                dependencies: RootDependencies()
            )
        }
    
    var body: some View {
        RootView(componentHolder.component)
            .onAppear { LifecycleRegistryExtKt.resume(self.componentHolder.lifecycle) }
            .onDisappear { LifecycleRegistryExtKt.stop(self.componentHolder.lifecycle) }
    }
}

private class RootDependencies: TodoRootDependencies {
    let database: TodoDatabase = TodoDatabaseCompanion().invoke(driver: TodoDatabaseDriverFactoryKt.TodoDatabaseDriver())
    
    let storeFactory: MvikotlinStoreFactory = DefaultStoreFactory()
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
