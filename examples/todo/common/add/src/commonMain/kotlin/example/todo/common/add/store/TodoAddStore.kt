package example.todo.common.add.store

import com.arkivanov.mvikotlin.core.store.Store
import example.todo.common.add.store.TodoAddStore.Intent
import example.todo.common.add.store.TodoAddStore.State

internal interface TodoAddStore : Store<Intent, State, Nothing> {

    sealed class Intent {
        data class SetText(val text: String) : Intent()
        object Add : Intent()
    }

    data class State(
        val text: String = ""
    )
}
