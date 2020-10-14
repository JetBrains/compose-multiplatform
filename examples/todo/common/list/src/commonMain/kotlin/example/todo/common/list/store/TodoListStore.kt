package example.todo.common.list.store

import com.arkivanov.mvikotlin.core.store.Store
import example.todo.common.list.store.TodoListStore.Intent
import example.todo.common.list.store.TodoListStore.State

internal interface TodoListStore : Store<Intent, State, Nothing> {

    sealed class Intent {
        data class SetDone(val id: Long, val isDone: Boolean) : Intent()
    }

    data class State(
        val items: List<TodoItem> = emptyList()
    )
}
