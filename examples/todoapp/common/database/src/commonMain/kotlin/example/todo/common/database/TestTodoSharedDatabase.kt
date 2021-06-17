package example.todo.common.database

import com.badoo.reaktive.base.invoke
import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.completable.completableFromFunction
import com.badoo.reaktive.completable.observeOn
import com.badoo.reaktive.maybe.Maybe
import com.badoo.reaktive.maybe.observeOn
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.observable.map
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.Scheduler
import com.badoo.reaktive.single.notNull
import com.badoo.reaktive.single.singleFromFunction
import com.badoo.reaktive.subject.behavior.BehaviorSubject

// There were problems when using real database in JS tests, hence the in-memory test implementation
class TestTodoSharedDatabase(
    private val scheduler: Scheduler
) : TodoSharedDatabase {

    private val itemsSubject = BehaviorSubject<Map<Long, TodoItemEntity>>(emptyMap())
    private val itemsObservable = itemsSubject.observeOn(scheduler)
    val testing: Testing = Testing()

    override fun observeAll(): Observable<List<TodoItemEntity>> =
        itemsObservable.map { it.values.toList() }

    override fun select(id: Long): Maybe<TodoItemEntity> =
        singleFromFunction { testing.select(id = id) }
            .notNull()
            .observeOn(scheduler)

    override fun add(text: String): Completable =
        execute { testing.add(text = text) }

    override fun setText(id: Long, text: String): Completable =
        execute { testing.setText(id = id, text = text) }

    override fun setDone(id: Long, isDone: Boolean): Completable =
        execute { testing.setDone(id = id, isDone = isDone) }

    override fun delete(id: Long): Completable =
        execute { testing.delete(id = id) }

    override fun clear(): Completable =
        execute { testing.clear() }

    private fun execute(block: () -> Unit): Completable =
        completableFromFunction(block)
            .observeOn(scheduler)

    inner class Testing {
        fun select(id: Long): TodoItemEntity? =
            itemsSubject.value[id]

        fun selectRequired(id: Long): TodoItemEntity =
            requireNotNull(select(id = id))

        fun add(text: String) {
            updateItems { items ->
                val nextId = items.keys.maxOrNull()?.plus(1L) ?: 1L

                val item =
                    TodoItemEntity(
                        id = nextId,
                        orderNum = items.size.toLong(),
                        text = text,
                        isDone = false
                    )

                items + (nextId to item)
            }
        }

        fun setText(id: Long, text: String) {
            updateItem(id = id) { it.copy(text = text) }
        }

        fun setDone(id: Long, isDone: Boolean) {
            updateItem(id = id) { it.copy(isDone = isDone) }
        }

        fun delete(id: Long) {
            updateItems { it - id }
        }

        fun clear() {
            updateItems { emptyMap() }
        }

        fun getLastInsertId(): Long? =
            itemsSubject.value.values.lastOrNull()?.id

        private fun updateItems(func: (Map<Long, TodoItemEntity>) -> Map<Long, TodoItemEntity>) {
            itemsSubject(func(itemsSubject.value))
        }

        private fun updateItem(id: Long, func: (TodoItemEntity) -> TodoItemEntity) {
            updateItems {
                it + (id to it.getValue(id).let(func))
            }
        }
    }
}
