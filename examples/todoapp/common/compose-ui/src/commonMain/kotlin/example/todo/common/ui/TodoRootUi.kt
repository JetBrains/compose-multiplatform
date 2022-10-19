@file:Suppress("EXPERIMENTAL_API_USAGE")

package example.todo.common.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import example.todo.common.root.TodoRoot
import example.todo.common.root.TodoRoot.Child

@Composable
fun TodoRootContent(component: TodoRoot) {
    Children(
        stack = component.childStack,
        animation = stackAnimation(fade() + scale()),
    ) {
        when (val child = it.instance) {
            is Child.Main -> TodoMainContent(child.component)
            is Child.Edit -> TodoEditContent(child.component)
        }
    }
}
