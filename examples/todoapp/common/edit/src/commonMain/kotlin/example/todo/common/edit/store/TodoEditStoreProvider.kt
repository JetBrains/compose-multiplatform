package example.todo.common.edit.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.reaktive.ReaktiveExecutor
import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.maybe.Maybe
import com.badoo.reaktive.maybe.map
import com.badoo.reaktive.maybe.observeOn
import com.badoo.reaktive.scheduler.mainScheduler
import example.todo.common.edit.TodoItem
import example.todo.common.edit.store.TodoEditStore.Intent
import example.todo.common.edit.store.TodoEditStore.Label
import example.todo.common.edit.store.TodoEditStore.State

internal class TodoEditStoreProvider(
    private val storeFactory: StoreFactory,
    private val database: Database,
    private val id: Long
) {

    fun provide(): TodoEditStore =
        object : TodoEditStore, Store<Intent, State, Label> by storeFactory.create(
            name = "EditStore",
            initialState = State(),
            bootstrapper = SimpleBootstrapper(Unit),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed class Result {
        data class Loaded(val item: TodoItem) : Result()
        data class TextChanged(val text: String) : Result()
        data class DoneChanged(val isDone: Boolean) : Result()
    }

    private inner class ExecutorImpl : ReaktiveExecutor<Intent, Unit, State, Result, Label>() {
        override fun executeAction(action: Unit, getState: () -> State) {
            database
                .load(id = id)
                .map(Result::Loaded)
                .observeOn(mainScheduler)
                .subscribeScoped(onSuccess = ::dispatch)
        }

        override fun executeIntent(intent: Intent, getState: () -> State) =
            when (intent) {
                is Intent.SetText -> setText(text = intent.text, state = getState())
                is Intent.SetDone -> setDone(isDone = intent.isDone, state = getState())
            }

        private fun setText(text: String, state: State) {
            dispatch(Result.TextChanged(text = text))
            publish(Label.Changed(TodoItem(text = text, isDone = state.isDone)))
            database.setText(id = id, text = text).subscribeScoped()
        }

        private fun setDone(isDone: Boolean, state: State) {
            dispatch(Result.DoneChanged(isDone = isDone))
            publish(Label.Changed(TodoItem(text = state.text, isDone = isDone)))
            database.setDone(id = id, isDone = isDone).subscribeScoped()
        }
    }

    private object ReducerImpl : Reducer<State, Result> {
        override fun State.reduce(result: Result): State =
            when (result) {
                is Result.Loaded -> copy(text = result.item.text, isDone = result.item.isDone)
                is Result.TextChanged -> copy(text = result.text)
                is Result.DoneChanged -> copy(isDone = result.isDone)
            }
    }

    interface Database {
        fun load(id: Long): Maybe<TodoItem>

        fun setText(id: Long, text: String): Completable

        fun setDone(id: Long, isDone: Boolean): Completable
    }
}
