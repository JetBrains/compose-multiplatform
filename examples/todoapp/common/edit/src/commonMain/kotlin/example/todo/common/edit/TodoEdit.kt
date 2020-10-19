package example.todo.common.edit

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.badoo.reaktive.base.Consumer
import example.todo.common.edit.TodoEdit.Dependencies
import example.todo.common.edit.integration.TodoEditImpl
import example.todo.common.utils.Component
import example.todo.database.TodoDatabase

interface TodoEdit : Component {

    interface Dependencies {
        val storeFactory: StoreFactory
        val database: TodoDatabase
        val itemId: Long
        val editOutput: Consumer<Output>
    }

    sealed class Output {
        object Finished : Output()
    }
}

@Suppress("FunctionName") // Factory function
fun TodoEdit(componentContext: ComponentContext, dependencies: Dependencies): TodoEdit =
    TodoEditImpl(componentContext, dependencies)
