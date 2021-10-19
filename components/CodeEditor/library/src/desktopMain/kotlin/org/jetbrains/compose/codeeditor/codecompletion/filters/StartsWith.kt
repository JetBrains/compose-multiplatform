package org.jetbrains.compose.codeeditor.codecompletion.filters

import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionIndexedElement

internal class StartsWith : Filter {
    override fun matches(
        element: CodeCompletionIndexedElement,
        prefix: String,
        ignoreCase: Boolean
    ) = element.element.name.startsWith(prefix, ignoreCase)
}
