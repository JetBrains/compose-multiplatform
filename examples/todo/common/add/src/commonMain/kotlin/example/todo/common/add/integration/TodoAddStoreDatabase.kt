package example.todo.common.add.integration

import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.completable.completableFromFunction
import com.badoo.reaktive.completable.subscribeOn
import com.badoo.reaktive.scheduler.ioScheduler
import example.todo.common.add.store.TodoAddStoreProvider.Database
import example.todo.common.database.TodoDatabaseQueries

internal class TodoAddStoreDatabase(
    private val queries: TodoDatabaseQueries
) : Database {

    override fun add(text: String): Completable =
        completableFromFunction {
            queries.transactionWithResult {
                queries.add(text = text)
                val lastId = queries.getLastInsertId().executeAsOne()
                queries.select(id = lastId).executeAsOne()
            }
        }
            .subscribeOn(ioScheduler)
}
