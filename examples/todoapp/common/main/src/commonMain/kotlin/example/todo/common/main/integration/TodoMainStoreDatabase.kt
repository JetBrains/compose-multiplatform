package example.todo.common.main.integration

import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.observable.mapIterable
import example.todo.common.database.TodoItemEntity
import example.todo.common.database.TodoSharedDatabase
import example.todo.common.main.TodoItem
import example.todo.common.main.store.TodoMainStoreProvider

internal class TodoMainStoreDatabase(
    private val database: TodoSharedDatabase
) : TodoMainStoreProvider.Database {

    override val updates: Observable<List<TodoItem>> =
        database
            .observeAll()
            .mapIterable { it.toItem() }

    private fun TodoItemEntity.toItem(): TodoItem =
        TodoItem(
            id = id,
            order = orderNum,
            text = text,
            isDone = isDone
        )

    override fun setDone(id: Long, isDone: Boolean): Completable =
        database.setDone(id = id, isDone = isDone)

    override fun delete(id: Long): Completable =
        database.delete(id = id)

    override fun add(text: String): Completable =
        database.add(text = text)
}
