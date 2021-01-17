package example.todo.common.main.store

import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.completable.completableFromFunction
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.subject.behavior.BehaviorSubject
import example.todo.common.main.TodoItem

internal class TestTodoMainStoreDatabase : TodoMainStoreProvider.Database {

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

    override fun delete(id: Long): Completable =
        completableFromFunction {
            this.items = items.filterNot { it.id == id }
        }

    override fun add(text: String): Completable =
        completableFromFunction {
            val id = items.maxByOrNull(TodoItem::id)?.id?.inc() ?: 1L
            this.items += TodoItem(id = id, order = id, text = text)
        }

    private fun update(id: Long, func: TodoItem.() -> TodoItem) {
        items = items.map { if (it.id == id) it.func() else it }
    }
}
