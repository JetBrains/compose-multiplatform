package example.todo.common.list.integration

import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.completable.completable
import com.badoo.reaktive.completable.subscribeOn
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.observable.mapIterable
import com.badoo.reaktive.scheduler.ioScheduler
import com.squareup.sqldelight.Query
import example.todo.common.database.TodoDatabaseQueries
import example.todo.common.database.TodoItemEntity
import example.todo.common.database.asObservable
import example.todo.common.list.store.TodoItem
import example.todo.common.list.store.TodoListStoreProvider.Database

internal class TodoListStoreDatabase(
    private val queries: TodoDatabaseQueries
) : Database {

    override val updates: Observable<List<TodoItem>> =
        queries
            .selectAll()
            .asObservable(Query<TodoItemEntity>::executeAsList)
            .mapIterable { it.toItem() }

    private fun TodoItemEntity.toItem(): TodoItem =
        TodoItem(
            id = id,
            order = orderNum,
            text = text,
            isDone = isDone
        )

    override fun setDone(id: Long, isDone: Boolean): Completable =
        completable { queries.setDone(id = id, isDone = isDone) }
            .subscribeOn(ioScheduler)
}
