package example.todo.common.list

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.badoo.reaktive.base.Consumer
import example.todo.common.list.TodoList.Dependencies
import example.todo.common.list.integration.TodoListImpl
import example.todo.common.utils.Component
import example.todo.database.TodoDatabase

interface TodoList : Component {

    interface Dependencies {
        val storeFactory: StoreFactory
        val database: TodoDatabase
        val listOutput: Consumer<Output>
    }

    sealed class Output {
        data class ItemSelected(val id: Long) : Output()
    }
}

@Suppress("FunctionName") // Factory function
fun TodoList(componentContext: ComponentContext, dependencies: Dependencies): TodoList =
    TodoListImpl(componentContext, dependencies)
