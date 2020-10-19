package example.todo.common.list.store

import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.completable.completableFromFunction
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.subject.behavior.BehaviorSubject
import example.todo.common.list.store.TodoListStoreProvider.Database

internal class TestTodoListStoreDatabase : Database {

    private val subject = BehaviorSubject<List<TodoItem>>(emptyList())

    var items: List<TodoItem>
        get() = subject.value
        set(value) {
            subject.onNext(value)
        }

    override val updates: Observable<List<TodoItem>> = subject

    override fun setDone(id: Long, isDone: Boolean): Completable =
        completableFromFunction {
            update(id = id) { copy(isDone = isDone) }
        }

    private fun update(id: Long, func: TodoItem.() -> TodoItem) {
        items = items.map { if (it.id == id) it.func() else it }
    }
}
