package example.todo.common.ui

import androidx.compose.runtime.Composable

fun <T, K> crossfade(): @Composable (currentChild: T, currentKey: K, children: @Composable (T, K) -> Unit) -> Unit =
    { currentChild: T, currentKey: K, children: @Composable (T, K) -> Unit ->
        KeyedCrossfade(currentChild, currentKey, children)
    }

@Composable
private fun <T, K> KeyedCrossfade(currentChild: T, currentKey: K, children: @Composable (T, K) -> Unit) {
    androidx.compose.animation.Crossfade(current = ChildWrapper(currentChild, currentKey)) {
        children(it.child, it.key)
    }
}

private class ChildWrapper<out T, out C>(val child: T, val key: C) {
    override fun equals(other: Any?): Boolean = key == (other as? ChildWrapper<*, *>)?.key
    override fun hashCode(): Int = key.hashCode()
}
