@file:Suppress("EXPERIMENTAL_API_USAGE")

package example.todo.common.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.crossfadeScale
import example.todo.common.root.TodoRoot
import example.todo.common.root.TodoRoot.Child

@Composable
fun TodoRootContent(component: TodoRoot) {
    Children(routerState = component.routerState, animation = crossfadeScale()) {
        when (val child = it.instance) {
            is Child.Main -> TodoMainContent(child.component)
            is Child.Edit -> TodoEditContent(child.component)
        }
    }
}
