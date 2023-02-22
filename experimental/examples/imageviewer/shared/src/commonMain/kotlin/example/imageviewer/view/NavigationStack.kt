package example.imageviewer.view

import androidx.compose.runtime.toMutableStateList

class NavigationStack<T>(initial: List<T>) {
    private val stack = initial.toMutableStateList()
    fun push(t: T) {
        stack.add(t)
    }

    fun back() {
        if(stack.size > 1) {
            // Always keep one element on the view stack
            stack.removeLast()
        }
    }

    fun lastWithIndex() = stack.withIndex().last()
}