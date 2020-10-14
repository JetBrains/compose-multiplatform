package example.todo.common.edit.integration

import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.completable.completableFromFunction
import com.badoo.reaktive.completable.subscribeOn
import com.badoo.reaktive.maybe.Maybe
import com.badoo.reaktive.maybe.map
import com.badoo.reaktive.maybe.maybeFromFunction
import com.badoo.reaktive.maybe.notNull
import com.badoo.reaktive.maybe.subscribeOn
import com.badoo.reaktive.scheduler.ioScheduler
import com.squareup.sqldelight.Query
import example.todo.common.database.TodoDatabaseQueries
import example.todo.common.database.TodoItemEntity
import example.todo.common.edit.TodoItem
import example.todo.common.edit.store.TodoEditStoreProvider.Database

internal class TodoEditStoreDatabase(
    private val queries: TodoDatabaseQueries
) : Database {

    override fun load(id: Long): Maybe<TodoItem> =
        maybeFromFunction { queries.select(id = id) }
            .subscribeOn(ioScheduler)
            .map(Query<TodoItemEntity>::executeAsOne)
            .notNull()
            .map { it.toItem() }

    private fun TodoItemEntity.toItem(): TodoItem =
        TodoItem(
            text = text,
            isDone = isDone
        )

    override fun setText(id: Long, text: String): Completable =
        completableFromFunction { queries.setText(id = id, text = text) }
            .subscribeOn(ioScheduler)

    override fun setDone(id: Long, isDone: Boolean): Completable =
        completableFromFunction { queries.setDone(id = id, isDone = isDone) }
            .subscribeOn(ioScheduler)
}
