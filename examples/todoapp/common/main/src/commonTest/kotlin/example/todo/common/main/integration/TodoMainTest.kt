package example.todo.common.main.integration

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.lifecycle.LifecycleRegistry
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.badoo.reaktive.scheduler.overrideSchedulers
import com.badoo.reaktive.subject.publish.PublishSubject
import com.badoo.reaktive.test.observable.assertValue
import com.badoo.reaktive.test.observable.test
import com.badoo.reaktive.test.scheduler.TestScheduler
import example.todo.common.database.TestDatabaseDriver
import example.todo.common.database.TodoItemEntity
import example.todo.common.main.TodoItem
import example.todo.common.main.TodoMain.Model
import example.todo.common.main.TodoMain.Output
import example.todo.database.TodoDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("TestFunctionName")
class TodoMainTest {

    private val lifecycle = LifecycleRegistry()
    private val database = TodoDatabase(TestDatabaseDriver())
    private val outputSubject = PublishSubject<Output>()
    private val output = outputSubject.test()

    private val queries = database.todoDatabaseQueries

    private val impl by lazy {
        TodoMainComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            storeFactory = DefaultStoreFactory,
            database = database,
            output = outputSubject
        )
    }

    private val model: Model get() = impl.models.value

    @BeforeTest
    fun before() {
        overrideSchedulers(
            main = { TestScheduler() },
            io = { TestScheduler() }
        )

        queries.clear()
    }

    @Test
    fun WHEN_item_added_to_database_THEN_item_displayed() {
        queries.add("Item1")

        assertEquals("Item1", firstItem().text)
    }

    @Test
    fun WHEN_item_deleted_from_database_THEN_item_not_displayed() {
        queries.add("Item1")
        val id = lastInsertItem().id

        queries.delete(id = id)

        assertFalse(model.items.any { it.id == id })
    }

    @Test
    fun WHEN_item_clicked_THEN_Output_Selected_emitted() {
        queries.add("Item1")
        val id = firstItem().id

        impl.onItemClicked(id = id)

        output.assertValue(Output.Selected(id = id))
    }

    @Test
    fun GIVEN_item_isDone_false_WHEN_done_changed_to_true_THEN_item_isDone_true_in_database() {
        queries.add("Item1")
        val id = firstItem().id
        queries.setDone(id = id, isDone = false)

        impl.onItemDoneChanged(id = id, isDone = true)

        assertTrue(queries.select(id = id).executeAsOne().isDone)
    }

    @Test
    fun GIVEN_item_isDone_true_WHEN_done_changed_to_false_THEN_item_isDone_false_in_database() {
        queries.add("Item1")
        val id = firstItem().id
        queries.setDone(id = id, isDone = true)

        impl.onItemDoneChanged(id = id, isDone = false)

        assertFalse(queries.select(id = id).executeAsOne().isDone)
    }

    @Test
    fun WHEN_item_delete_clicked_THEN_item_deleted_in_database() {
        queries.add("Item1")
        val id = firstItem().id

        impl.onItemDeleteClicked(id = id)

        assertNull(queries.select(id = id).executeAsOneOrNull())
    }

    @Test
    fun WHEN_item_text_changed_in_database_THEN_item_updated() {
        queries.add("Item1")
        val id = firstItem().id

        queries.setText(id = id, text = "New text")

        assertEquals("New text", firstItem().text)
    }

    @Test
    fun WHEN_input_text_changed_THEN_text_updated() {
        impl.onInputTextChanged(text = "Item text")

        assertEquals("Item text", model.text)
    }

    @Test
    fun GIVEN_input_text_entered_WHEN_add_item_clicked_THEN_item_added_in_database() {
        impl.onInputTextChanged(text = "Item text")

        impl.onAddItemClicked()

        assertEquals("Item text", lastInsertItem().text)
    }

    private fun firstItem(): TodoItem = model.items[0]

    private fun lastInsertItem(): TodoItemEntity {
        val lastInsertId = queries.transactionWithResult<Long> { queries.getLastInsertId().executeAsOne() }

        return queries.select(id = lastInsertId).executeAsOne()
    }
}
