package example.todo.common.main.integration

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.lifecycle.LifecycleRegistry
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.badoo.reaktive.base.Consumer
import com.badoo.reaktive.scheduler.overrideSchedulers
import com.badoo.reaktive.subject.publish.PublishSubject
import com.badoo.reaktive.test.observable.assertValue
import com.badoo.reaktive.test.observable.test
import com.badoo.reaktive.test.scheduler.TestScheduler
import example.todo.common.database.TestDatabaseDriver
import example.todo.common.database.TodoItemEntity
import example.todo.common.main.TodoMain.Dependencies
import example.todo.common.main.TodoMain.Output
import example.todo.common.main.store.TodoItem
import example.todo.common.main.store.TodoMainStore.Intent
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
        TodoMainImpl(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            dependencies = object : Dependencies {
                override val storeFactory: StoreFactory = DefaultStoreFactory
                override val database: TodoDatabase = this@TodoMainTest.database
                override val mainOutput: Consumer<Output> = outputSubject
            }
        )
    }

    @BeforeTest
    fun before() {
        overrideSchedulers(
            main = { TestScheduler() },
            io = { TestScheduler() }
        )
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

        assertFalse(impl.state.items.any { it.id == id })
    }

    @Test
    fun WHEN_item_selected_THEN_Output_Selected_emitted() {
        queries.add("Item1")
        val id = firstItem().id

        impl.onOutput(Output.Selected(id = id))

        output.assertValue(Output.Selected(id = id))
    }

    @Test
    fun GIVEN_item_isDone_false_WHEN_done_changed_to_true_THEN_item_isDone_true_in_database() {
        queries.add("Item1")
        val id = firstItem().id
        queries.setDone(id = id, isDone = false)

        impl.onIntent(Intent.SetItemDone(id = id, isDone = true))

        assertTrue(queries.select(id = id).executeAsOne().isDone)
    }

    @Test
    fun GIVEN_item_isDone_true_WHEN_done_changed_to_false_THEN_item_isDone_false_in_database() {
        queries.add("Item1")
        val id = firstItem().id
        queries.setDone(id = id, isDone = true)

        impl.onIntent(Intent.SetItemDone(id = id, isDone = false))

        assertFalse(queries.select(id = id).executeAsOne().isDone)
    }

    @Test
    fun WHEN_delete_clicked_THEN_item_deleted_in_database() {
        queries.add("Item1")
        val id = firstItem().id

        impl.onIntent(Intent.DeleteItem(id = id))

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
    fun WHEN_text_changed_THEN_text_updated() {
        impl.onIntent(Intent.SetText(text = "Item text"))

        assertEquals("Item text", impl.state.text)
    }

    @Test
    fun GIVEN_text_entered_WHEN_add_clicked_THEN_item_added_in_database() {
        impl.onIntent(Intent.SetText(text = "Item text"))

        impl.onIntent(Intent.AddItem)

        assertEquals("Item text", lastInsertItem().text)
    }

    private fun firstItem(): TodoItem = impl.state.items[0]

    private fun lastInsertItem(): TodoItemEntity {
        val lastInsertId = queries.getLastInsertId().executeAsOne()

        return queries.select(id = lastInsertId).executeAsOne()
    }
}
