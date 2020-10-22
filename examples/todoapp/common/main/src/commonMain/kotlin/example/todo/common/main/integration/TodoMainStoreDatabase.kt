package example.todo.common.main.integration

import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.completable.completableFromFunction
import com.badoo.reaktive.completable.subscribeOn
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.observable.mapIterable
import com.badoo.reaktive.scheduler.ioScheduler
import com.squareup.sqldelight.Query
import example.todo.common.database.TodoDatabaseQueries
import example.todo.common.database.TodoItemEntity
import example.todo.common.database.asObservable
import example.todo.common.main.store.TodoItem
import example.todo.common.main.store.TodoMainStoreProvider

internal class TodoMainStoreDatabase(
    private val queries: TodoDatabaseQueries
) : TodoMainStoreProvider.Database {

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
        completableFromFunction { queries.setDone(id = id, isDone = isDone) }
            .subscribeOn(ioScheduler)

    override fun delete(id: Long): Completable =
        completableFromFunction { queries.delete(id = id) }
            .subscribeOn(ioScheduler)

    override fun add(text: String): Completable =
        completableFromFunction {
            queries.transactionWithResult {
                queries.add(text = text)
                val lastId = queries.getLastInsertId().executeAsOne()
                queries.select(id = lastId).executeAsOne()
            }
        }.subscribeOn(ioScheduler)
}
