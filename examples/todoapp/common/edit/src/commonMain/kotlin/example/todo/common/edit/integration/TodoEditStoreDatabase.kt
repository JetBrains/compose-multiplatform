package example.todo.common.edit.integration

import com.badoo.reaktive.completable.Completable
import com.badoo.reaktive.maybe.Maybe
import com.badoo.reaktive.maybe.map
import example.todo.common.database.TodoItemEntity
import example.todo.common.database.TodoSharedDatabase
import example.todo.common.edit.TodoItem
import example.todo.common.edit.store.TodoEditStoreProvider.Database

internal class TodoEditStoreDatabase(
    private val database: TodoSharedDatabase
) : Database {

    override fun load(id: Long): Maybe<TodoItem> =
        database
            .select(id = id)
            .map { it.toItem() }

    private fun TodoItemEntity.toItem(): TodoItem =
        TodoItem(
            text = text,
            isDone = isDone
        )

    override fun setText(id: Long, text: String): Completable =
        database.setText(id = id, text = text)

    override fun setDone(id: Long, isDone: Boolean): Completable =
        database.setDone(id = id, isDone = isDone)
}
