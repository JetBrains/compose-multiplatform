package example.todo.common.edit.store

import com.arkivanov.mvikotlin.core.store.Store
import example.todo.common.edit.TodoItem
import example.todo.common.edit.store.TodoEditStore.Intent
import example.todo.common.edit.store.TodoEditStore.Label
import example.todo.common.edit.store.TodoEditStore.State

internal interface TodoEditStore : Store<Intent, State, Label> {

    sealed class Intent {
        data class SetText(val text: String) : Intent()
        data class SetDone(val isDone: Boolean) : Intent()
    }

    data class State(
        val text: String = "",
        val isDone: Boolean = false
    )

    sealed class Label {
        data class Changed(val item: TodoItem) : Label()
    }
}
