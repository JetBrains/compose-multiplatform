package example.todo.common.main.store

import com.arkivanov.mvikotlin.core.store.Store
import example.todo.common.main.TodoItem
import example.todo.common.main.store.TodoMainStore.Intent
import example.todo.common.main.store.TodoMainStore.State

internal interface TodoMainStore : Store<Intent, State, Nothing> {

    sealed class Intent {
        data class SetItemDone(val id: Long, val isDone: Boolean) : Intent()
        data class DeleteItem(val id: Long) : Intent()
        data class SetText(val text: String) : Intent()
        object AddItem : Intent()
    }

    data class State(
        val items: List<TodoItem> = emptyList(),
        val text: String = ""
    )
}
