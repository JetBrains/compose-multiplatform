package org.jetbrains.compose.html2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.html2.internal.*

fun composeHtmlToString(
    content: @Composable () -> Unit
): String {
    val rootElement = HtmlElementStringNode.root()
    val recomposer = Recomposer(Dispatchers.Unconfined)
    val composition = ControlledComposition(
        HtmlApplier(root = HtmlStringNodeWrapper(rootElement)),
        parent = recomposer
    )

    composition.setContent {
        CompositionLocalProvider(LocalComposeHtml2Context provides StringBasedComposeHtml2Context) {
            content()
        }
    }

    val result = try {
        rootElement.toHtmlString()
    } finally {
        composition.dispose()
    }

    return result
}

@Composable
fun Div(
    attrsScope: AttrsScope.() -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    LocalComposeHtml2Context.current.TagElement("div", attrsScope, content)
}

@Composable
fun Text(value: String) {
    LocalComposeHtml2Context.current.TextElement(value)
}