package example.todo.common.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import example.todo.common.root.TodoRoot
import example.todo.common.root.TodoRoot.Child

@Composable
fun TodoRootContent(component: TodoRoot) {
    // Crossfade does not preserve UI state properly since (probably) 0.3.0-build146.
    // Uncomment when https://issuetracker.google.com/u/1/issues/178729296 is fixed.
    Children(routerState = component.routerState /*, animation = crossfade()*/) { child, _ ->
        when (child) {
            is Child.Main -> TodoMainContent(child.component)
            is Child.Edit -> TodoEditContent(child.component)
        }
    }
}
