package org.jetbrains.compose.codeeditor.codecompletion.filters

import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionIndexedElement

internal class Contains : Filter {
    override fun matches(
        element: CodeCompletionIndexedElement,
        prefix: String,
        ignoreCase: Boolean
    ): Boolean = element.element.name.contains(prefix, ignoreCase)
}
