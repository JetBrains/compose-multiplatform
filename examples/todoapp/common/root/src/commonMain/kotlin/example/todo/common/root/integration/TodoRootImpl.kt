package example.todo.common.root.integration

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router
import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize
import com.badoo.reaktive.base.Consumer
import example.todo.common.edit.TodoEdit
import example.todo.common.main.TodoMain
import example.todo.common.root.TodoRoot
import example.todo.common.root.TodoRoot.Dependencies
import example.todo.common.utils.Component
import example.todo.common.utils.Consumer
import example.todo.common.utils.Crossfade
import example.todo.common.utils.children

internal class TodoRootImpl(
    componentContext: ComponentContext,
    dependencies: Dependencies
) : TodoRoot, ComponentContext by componentContext, Dependencies by dependencies {

    private val router =
        router<Configuration, Component>(
            initialConfiguration = Configuration.Main,
            handleBackButton = true,
            componentFactory = ::createChild
        )

    private fun createChild(configuration: Configuration, componentContext: ComponentContext): Component =
        when (configuration) {
            is Configuration.Main -> todoMain(componentContext)
            is Configuration.Edit -> todoEdit(componentContext, itemId = configuration.itemId)
        }

    private fun todoMain(componentContext: ComponentContext): TodoMain =
        TodoMain(
            componentContext = componentContext,
            dependencies = object : TodoMain.Dependencies, Dependencies by this {
                override val mainOutput: Consumer<TodoMain.Output> = Consumer(::onMainOutput)
            }
        )

    private fun todoEdit(componentContext: ComponentContext, itemId: Long): TodoEdit =
        TodoEdit(
            componentContext = componentContext,
            dependencies = object : TodoEdit.Dependencies, Dependencies by this {
                override val itemId: Long = itemId
                override val editOutput: Consumer<TodoEdit.Output> = Consumer(::onEditOutput)
            }
        )

    private fun onMainOutput(output: TodoMain.Output): Unit =
        when (output) {
            is TodoMain.Output.Selected -> router.push(Configuration.Edit(itemId = output.id))
        }

    private fun onEditOutput(output: TodoEdit.Output): Unit =
        when (output) {
            is TodoEdit.Output.Finished -> router.pop()
        }

    @Composable
    override fun invoke() {
        router.state.children { child, configuration ->
            Crossfade(currentChild = child, currentKey = configuration) { currentChild ->
                currentChild()
            }
        }
    }

    private sealed class Configuration : Parcelable {
        @Parcelize
        object Main : Configuration()

        @Parcelize
        data class Edit(val itemId: Long) : Configuration()
    }
}
