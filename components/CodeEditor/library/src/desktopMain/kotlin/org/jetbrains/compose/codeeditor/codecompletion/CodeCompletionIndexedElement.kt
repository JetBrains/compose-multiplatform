package org.jetbrains.compose.codeeditor.codecompletion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.codeeditor.CodeCompletionElement

internal class CodeCompletionIndexedElement(
    val id: Int,
    val element: CodeCompletionElement,
    val onClick: () -> Unit = {},
    val onDoubleClick: () -> Unit = {}
) {
    var selected by mutableStateOf(false)
        private set

    fun select() {
        selected = true
    }

    fun unselect() {
        selected = false
    }

    override fun toString(): String {
        return "CodeCompletionIndexedElement(id=$id, element=$element)"
    }

}
