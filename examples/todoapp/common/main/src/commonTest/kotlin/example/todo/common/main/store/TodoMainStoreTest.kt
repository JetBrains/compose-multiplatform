package example.todo.common.main.store

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.badoo.reaktive.scheduler.overrideSchedulers
import com.badoo.reaktive.test.scheduler.TestScheduler
import example.todo.common.main.TodoItem
import example.todo.common.main.store.TodoMainStore.Intent
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("TestFunctionName")
class TodoMainStoreTest {

    private val database = TestTodoMainStoreDatabase()
    private val provider = TodoMainStoreProvider(storeFactory = DefaultStoreFactory, database = database)

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
    fun WHEN_Intent_SetItemDone_THEN_done_changed_in_state() {
        val item1 = TodoItem(id = 1L, text = "item1")
        val item2 = TodoItem(id = 2L, text = "item2", isDone = false)
        database.items = listOf(item1, item2)
        val store = provider.provide()

        store.accept(Intent.SetItemDone(id = 2L, isDone = true))

        assertTrue(store.state.items.first { it.id == 2L }.isDone)
    }

    @Test
    fun WHEN_Intent_SetItemDone_THEN_done_changed_in_database() {
        val item1 = TodoItem(id = 1L, text = "item1")
        val item2 = TodoItem(id = 2L, text = "item2", isDone = false)
        database.items = listOf(item1, item2)
        val store = provider.provide()

        store.accept(Intent.SetItemDone(id = 2L, isDone = true))

        assertTrue(database.items.first { it.id == 2L }.isDone)
    }

    @Test
    fun WHEN_Intent_DeleteItem_THEN_item_deleted_in_state() {
        val item1 = TodoItem(id = 1L, text = "item1")
        val item2 = TodoItem(id = 2L, text = "item2")
        database.items = listOf(item1, item2)
        val store = provider.provide()

        store.accept(Intent.DeleteItem(id = 2L))

        assertFalse(store.state.items.any { it.id == 2L })
    }

    @Test
    fun WHEN_Intent_DeleteItem_THEN_item_deleted_in_database() {
        val item1 = TodoItem(id = 1L, text = "item1")
        val item2 = TodoItem(id = 2L, text = "item2")
        database.items = listOf(item1, item2)
        val store = provider.provide()

        store.accept(Intent.DeleteItem(id = 2L))

        assertFalse(database.items.any { it.id == 2L })
    }

    @Test
    fun WHEN_Intent_SetText_WHEN_text_changed_in_state() {
        val store = provider.provide()

        store.accept(Intent.SetText(text = "Item text"))

        assertEquals("Item text", store.state.text)
    }

    @Test
    fun GIVEN_text_entered_WHEN_Intent_AddItem_THEN_item_added_in_database() {
        val store = provider.provide()
        store.accept(Intent.SetText(text = "Item text"))

        store.accept(Intent.AddItem)

        assertTrue(database.items.any { it.text == "Item text" })
    }

    @Test
    fun GIVEN_text_entered_WHEN_Intent_AddItem_THEN_text_cleared_in_state() {
        val store = provider.provide()
        store.accept(Intent.SetText(text = "Item text"))

        store.accept(Intent.AddItem)

        assertEquals("", store.state.text)
    }

    @Test
    fun GIVEN_no_text_entered_WHEN_Intent_AddItem_THEN_item_not_added() {
        val store = provider.provide()

        store.accept(Intent.AddItem)

        assertEquals(0, store.state.items.size)
    }
}
