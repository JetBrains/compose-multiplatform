package example.todo.common.main.integration

import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.child
import com.arkivanov.mvikotlin.core.binder.BinderLifecycleMode
import com.badoo.reaktive.base.Consumer
import com.badoo.reaktive.observable.mapNotNull
import com.badoo.reaktive.subject.publish.PublishSubject
import example.todo.common.add.TodoAdd
import example.todo.common.list.TodoList
import example.todo.common.main.TodoMain
import example.todo.common.main.TodoMain.Dependencies
import example.todo.common.utils.bind

internal class TodoMainImpl(
    componentContext: ComponentContext,
    dependencies: Dependencies
) : TodoMain, ComponentContext by componentContext, Dependencies by dependencies {

    private val listOutput = PublishSubject<TodoList.Output>()

    private val todoList =
        TodoList(
            componentContext = child(key = "TodoList"),
            dependencies = object : TodoList.Dependencies, Dependencies by dependencies {
                override val listOutput: Consumer<TodoList.Output> = this@TodoMainImpl.listOutput
            }
        )

    private val todoAdd =
        TodoAdd(
            componentContext = child(key = "TodoAdd"),
            dependencies = object : TodoAdd.Dependencies, Dependencies by dependencies {}
        )

    init {
        bind(BinderLifecycleMode.START_STOP) {
            listOutput.mapNotNull(listOutputToOutput) bindTo mainOutput
        }
    }

    @Composable
    override fun invoke() {
        Column {
            TopAppBar(title = { Text(text = "Todo List") })

            Box(Modifier.weight(1F)) {
                todoList()
            }
            todoAdd()
        }
    }
}
