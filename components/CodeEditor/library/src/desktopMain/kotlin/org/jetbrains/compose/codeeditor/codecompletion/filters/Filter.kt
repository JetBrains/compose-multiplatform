package org.jetbrains.compose.codeeditor.codecompletion.filters

import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionIndexedElement

internal interface Filter {
    fun matches(
        element: CodeCompletionIndexedElement,
        prefix: String,
        ignoreCase: Boolean = true
    ): Boolean
}
