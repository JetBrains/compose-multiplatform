package example.todo.common.add

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.store.StoreFactory
import example.todo.common.add.TodoAdd.Dependencies
import example.todo.common.add.integration.TodoAddImpl
import example.todo.common.utils.Component
import example.todo.database.TodoDatabase

interface TodoAdd : Component {

    interface Dependencies {
        val storeFactory: StoreFactory
        val database: TodoDatabase
    }
}

@Suppress("FunctionName") // Factory function
fun TodoAdd(componentContext: ComponentContext, dependencies: Dependencies): TodoAdd =
    TodoAddImpl(componentContext, dependencies)
