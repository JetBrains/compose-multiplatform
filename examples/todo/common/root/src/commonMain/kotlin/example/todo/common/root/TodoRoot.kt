package example.todo.common.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.store.StoreFactory
import example.todo.common.root.TodoRoot.Dependencies
import example.todo.common.root.integration.TodoRootImpl
import example.todo.common.utils.Component
import example.todo.database.TodoDatabase

interface TodoRoot : Component {

    interface Dependencies {
        val storeFactory: StoreFactory
        val database: TodoDatabase
    }
}

@Suppress("FunctionName") // Factory function
fun TodoRoot(componentContext: ComponentContext, dependencies: Dependencies): TodoRoot =
    TodoRootImpl(componentContext, dependencies)
