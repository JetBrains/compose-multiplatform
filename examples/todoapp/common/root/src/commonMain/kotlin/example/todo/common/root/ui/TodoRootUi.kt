package example.todo.common.root.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.jetbrains.children
import example.todo.common.edit.ui.TodoEditContent
import example.todo.common.main.ui.TodoMainContent
import example.todo.common.root.TodoRoot
import example.todo.common.root.TodoRoot.Child
import example.todo.common.utils.Crossfade

@Composable
fun TodoRootContent(component: TodoRoot) {
    component.routerState.children { child, configuration ->
        Crossfade(currentChild = child, currentKey = configuration) { currentChild ->
            when (currentChild) {
                is Child.Main -> TodoMainContent(currentChild.component)
                is Child.Edit -> TodoEditContent(currentChild.component)
            }
        }
    }
}
