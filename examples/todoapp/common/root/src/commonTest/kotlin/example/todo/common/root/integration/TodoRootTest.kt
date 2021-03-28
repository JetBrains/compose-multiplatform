package example.todo.common.root.integration

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.lifecycle.LifecycleRegistry
import com.arkivanov.decompose.lifecycle.resume
import com.badoo.reaktive.base.invoke
import example.todo.common.edit.TodoEdit
import example.todo.common.main.TodoMain
import example.todo.common.root.TodoRoot
import example.todo.common.root.TodoRoot.Child
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("TestFunctionName")
class TodoRootTest {

    private val lifecycle = LifecycleRegistry().apply { resume() }

    @Test
    fun WHEN_created_THEN_TodoMain_displayed() {
        val root = root()

        assertTrue(root.activeChild is Child.Main)
    }

    @Test
    fun GIVEN_TodoMain_displayed_WHEN_TodoMain_Output_Selected_THEN_TodoEdit_displayed() {
        val root = root()

        root.activeChild.asTodoMain().output(TodoMain.Output.Selected(id = 10L))

        assertTrue(root.activeChild is Child.Edit)
        assertEquals(10L, root.activeChild.asTodoEdit().itemId)
    }

    @Test
    fun GIVEN_TodoEdit_displayed_WHEN_TodoEdit_Output_Finished_THEN_TodoMain_displayed() {
        val root = root()
        root.activeChild.asTodoMain().output(TodoMain.Output.Selected(id = 10L))

        root.activeChild.asTodoEdit().output(TodoEdit.Output.Finished)

        assertTrue(root.activeChild is Child.Main)
    }

    private fun root(): TodoRoot =
        TodoRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            todoMain = { _, output -> TodoMainFake(output) },
            todoEdit = { _, itemId, output -> TodoEditFake(itemId, output) }
        )

    private val TodoRoot.activeChild: Child get() = routerState.value.activeChild.instance

    private val Child.component: Any
        get() =
            when (this) {
                is Child.Main -> component
                is Child.Edit -> component
            }

    private fun Child.asTodoMain(): TodoMainFake = component as TodoMainFake

    private fun Child.asTodoEdit(): TodoEditFake = component as TodoEditFake
}
