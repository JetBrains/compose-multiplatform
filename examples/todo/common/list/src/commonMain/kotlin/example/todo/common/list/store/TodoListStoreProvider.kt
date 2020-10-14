package example.todo.common.list.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.reaktive.ReaktiveExecutor
import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.observable.map
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.mainScheduler
import example.todo.common.list.store.TodoListStore.Intent
import example.todo.common.list.store.TodoListStore.State

internal class TodoListStoreProvider(
    private val storeFactory: StoreFactory,
    private val database: Database
) {

    fun provide(): TodoListStore =
        object : TodoListStore, Store<Intent, State, Nothing> by storeFactory.create(
            name = "TodoListStore",
            initialState = State(),
            bootstrapper = SimpleBootstrapper(Unit),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed class Result {
        data class ItemsLoaded(val items: List<TodoItem>) : Result()
        data class DoneChanged(val id: Long, val isDone: Boolean) : Result()
    }

    private inner class ExecutorImpl : ReaktiveExecutor<Intent, Unit, State, Result, Nothing>() {
        override fun executeAction(action: Unit, getState: () -> State) {
            database
                .updates
                .observeOn(mainScheduler)
                .map(Result::ItemsLoaded)
                .subscribeScoped(onNext = ::dispatch)
        }

        override fun executeIntent(intent: Intent, getState: () -> State): Unit =
            when (intent) {
                is Intent.SetDone -> setDone(id = intent.id, isDone = intent.isDone)
            }

        private fun setDone(id: Long, isDone: Boolean) {
            dispatch(Result.DoneChanged(id = id, isDone = isDone))
            database.setDone(id = id, isDone = isDone).subscribeScoped()
        }
    }

    private object ReducerImpl : Reducer<State, Result> {
        override fun State.reduce(result: Result): State =
            when (result) {
                is Result.ItemsLoaded -> copy(items = result.items.sorted())
                is Result.DoneChanged -> update(id = result.id) { copy(isDone = result.isDone) }
            }

        private inline fun State.update(id: Long, func: TodoItem.() -> TodoItem): State {
            val item = items.find { it.id == id } ?: return this

            return put(item.func())
        }

        private fun State.put(item: TodoItem): State {
            val oldItems = items.associateByTo(mutableMapOf(), TodoItem::id)
            val oldItem: TodoItem? = oldItems.put(item.id, item)

            return copy(items = if (oldItem?.order == item.order) oldItems.values.toList() else oldItems.values.sorted())
        }

        private fun Iterable<TodoItem>.sorted(): List<TodoItem> = sortedByDescending(TodoItem::order)
    }

    interface Database {
        val updates: Observable<List<TodoItem>>

        fun setDone(id: Long, isDone: Boolean): Completable
    }
}
