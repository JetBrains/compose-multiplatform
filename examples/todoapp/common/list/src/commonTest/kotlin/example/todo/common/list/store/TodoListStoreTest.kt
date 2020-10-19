package example.todo.common.list.store

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.badoo.reaktive.scheduler.overrideSchedulers
import com.badoo.reaktive.test.scheduler.TestScheduler
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("TestFunctionName")
class TodoListStoreTest {

    private val database = TestTodoListStoreDatabase()
    private val provider = TodoListStoreProvider(storeFactory = DefaultStoreFactory, database = database)

    @BeforeTest
    fun before() {
        overrideSchedulers(main = { TestScheduler() })
    }

    @Test
    fun GIVEN_items_in_database_WHEN_created_THEN_loads_items_from_database() {
        val item1 = TodoItem(id = 1L, order = 2L, text = "item1")
        val item2 = TodoItem(id = 2L, order = 1L, text = "item2", isDone = true)
        val item3 = TodoItem(id = 3L, order = 3L, text = "item3")
        database.items = listOf(item1, item2, item3)

        val store = provider.provide()

        assertEquals(listOf(item3, item1, item2), store.state.items)
    }

    @Test
    fun WHEN_items_changed_in_database_THEN_contains_new_items() {
        database.items = listOf(TodoItem())
        val store = provider.provide()

        val item1 = TodoItem(id = 1L, order = 2L, text = "item1")
        val item2 = TodoItem(id = 2L, order = 1L, text = "item2", isDone = true)
        val item3 = TodoItem(id = 3L, order = 3L, text = "item3")
        database.items = listOf(item1, item2, item3)

        assertEquals(listOf(item3, item1, item2), store.state.items)
    }

    @Test
    fun WHEN_Intent_setDone_THEN_done_changed_in_state() {
        val item1 = TodoItem(id = 1L, text = "item1")
        val item2 = TodoItem(id = 2L, text = "item2", isDone = false)
        database.items = listOf(item1, item2)
        val store = provider.provide()

        store.accept(TodoListStore.Intent.SetDone(id = 2L, isDone = true))

        assertTrue(store.state.items.first { it.id == 2L }.isDone)
    }
}
