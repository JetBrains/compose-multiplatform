package example.todo.common.list.integration

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
import example.todo.common.list.TodoList.Dependencies
import example.todo.common.list.TodoList.Output
import example.todo.common.list.store.TodoItem
import example.todo.database.TodoDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("TestFunctionName")
class TodoListTest {

    private val lifecycle = LifecycleRegistry()
    private val database = TodoDatabase(TestDatabaseDriver())
    private val outputSubject = PublishSubject<Output>()
    private val output = outputSubject.test()

    private val queries = database.todoDatabaseQueries

    private val impl by lazy {
        TodoListImpl(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            dependencies = object : Dependencies {
                override val storeFactory: StoreFactory = DefaultStoreFactory
                override val database: TodoDatabase = this@TodoListTest.database
                override val listOutput: Consumer<Output> = outputSubject
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
    fun WHEN_item_clicked_THEN_Output_ItemSelected_emitted() {
        queries.add("Item1")
        val id = firstItem().id

        impl.onItemClicked(id = id)

        output.assertValue(Output.ItemSelected(id = id))
    }

    @Test
    fun GIVEN_item_isDone_false_WHEN_done_changed_to_true_THEN_item_isDone_true() {
        queries.add("Item1")
        val id = firstItem().id
        queries.setDone(id = id, isDone = false)

        impl.onDoneChanged(id = id, isDone = true)

        assertTrue(firstItem().isDone)
    }

    @Test
    fun GIVEN_item_isDone_true_WHEN_done_changed_to_false_THEN_item_isDone_false() {
        queries.add("Item1")
        val id = firstItem().id
        queries.setDone(id = id, isDone = true)

        impl.onDoneChanged(id = id, isDone = false)

        assertFalse(firstItem().isDone)
    }

    @Test
    fun WHEN_item_text_changed_THEN_item_updated() {
        queries.add("Item1")
        val id = firstItem().id

        queries.setText(id = id, text = "New text")

        assertEquals("New text", firstItem().text)
    }

    private fun firstItem(): TodoItem = impl.state.items[0]
}
