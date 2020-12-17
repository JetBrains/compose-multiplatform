package example.todo.common.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.badoo.reaktive.base.Consumer
import example.todo.common.main.TodoMain.Dependencies
import example.todo.common.main.integration.TodoMainImpl
import example.todo.database.TodoDatabase

interface TodoMain {

    val models: Value<Model>

    fun onItemClicked(id: Long)

    fun onItemDoneChanged(id: Long, isDone: Boolean)

    fun onItemDeleteClicked(id: Long)

    fun onInputTextChanged(text: String)

    fun onAddItemClicked()

    data class Model(
        val items: List<TodoItem>,
        val text: String
    )

    interface Dependencies {
        val storeFactory: StoreFactory
        val database: TodoDatabase
        val mainOutput: Consumer<Output>
    }

    sealed class Output {
        data class Selected(val id: Long) : Output()
    }
}

@Suppress("FunctionName") // Factory function
fun TodoMain(componentContext: ComponentContext, dependencies: Dependencies): TodoMain =
    TodoMainImpl(componentContext, dependencies)
