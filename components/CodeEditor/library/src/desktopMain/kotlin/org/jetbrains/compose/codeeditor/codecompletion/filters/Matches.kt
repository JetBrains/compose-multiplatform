package org.jetbrains.compose.codeeditor.codecompletion.filters

import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionIndexedElement

internal class Matches : Filter {
    override fun matches(
        element: CodeCompletionIndexedElement,
        prefix: String,
        ignoreCase: Boolean
    ) = element.element.name.equals(prefix, ignoreCase)
}
