package org.jetbrains.compose.html2

import androidx.compose.runtime.Composable
import org.jetbrains.compose.html2.internal.LocalComposeHtml2Context



@Composable
fun Div(
    attrsScope: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    LocalComposeHtml2Context.current.TagElement("div", attrsScope, content)
}

@Composable
fun Text(value: String) {
    LocalComposeHtml2Context.current.TextElement(value)
}