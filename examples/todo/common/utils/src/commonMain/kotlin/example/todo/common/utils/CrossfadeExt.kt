package example.todo.common.utils

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable

@Composable
fun <T> Crossfade(currentChild: T, currentKey: Any, children: @Composable() (T) -> Unit) {
    Crossfade(current = ChildWrapper(currentChild, currentKey)) {
        children(it.child)
    }
}

private class ChildWrapper<out T>(val child: T, val key: Any) {
    override fun equals(other: Any?): Boolean = key == (other as? ChildWrapper<*>)?.key
    override fun hashCode(): Int = key.hashCode()
}
