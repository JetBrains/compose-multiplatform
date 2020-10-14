package example.todo.common.add.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.reaktive.ReaktiveExecutor
import com.badoo.reaktive.completable.Completable
import example.todo.common.add.store.TodoAddStore.Intent
import example.todo.common.add.store.TodoAddStore.State

internal class TodoAddStoreProvider(
    private val storeFactory: StoreFactory,
    private val database: Database
) {

    fun provide(): TodoAddStore =
        object : TodoAddStore, Store<Intent, State, Nothing> by storeFactory.create(
            name = "TodoAddStore",
            initialState = State(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed class Result {
        data class TextChanged(val text: String) : Result()
    }

    private inner class ExecutorImpl : ReaktiveExecutor<Intent, Nothing, State, Result, Nothing>() {
        override fun executeIntent(intent: Intent, getState: () -> State): Unit =
            when (intent) {
                is Intent.SetText -> dispatch(Result.TextChanged(text = intent.text))
                is Intent.Add -> add(state = getState())
            }

        private fun add(state: State) {
            dispatch(Result.TextChanged(text = ""))
            database.add(text = state.text).subscribeScoped()
        }
    }

    private object ReducerImpl : Reducer<State, Result> {
        override fun State.reduce(result: Result): State =
            when (result) {
                is Result.TextChanged -> copy(text = result.text)
            }
    }

    interface Database {
        fun add(text: String): Completable
    }
}
