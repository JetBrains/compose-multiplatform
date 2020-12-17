package example.todo.common.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import example.todo.common.root.TodoRoot
import example.todo.common.root.TodoRoot.Child

@Composable
fun TodoRootContent(component: TodoRoot) {
    Children(routerState = component.routerState, animation = crossfade()) { child, _ ->
        when (child) {
            is Child.Main -> TodoMainContent(child.component)
            is Child.Edit -> TodoEditContent(child.component)
        }
    }
}
