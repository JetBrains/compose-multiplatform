package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext
import org.jetbrains.compose.web.dom.StringComposeHtmlContext
import org.jetbrains.compose.web.dom.StringHtmlApplier
import org.jetbrains.compose.web.dom.StringHtmlElementNode
import org.jetbrains.compose.web.dom.StringHtmlNodeWrapper

/**
 * Composes [content] once into an HTML string without creating browser DOM nodes.
 * The backing composition is disposed after the initial HTML has been serialized.
 */
fun composeHtmlToString(
    content: @Composable () -> Unit
): String {
    val root = StringHtmlElementNode.root()
    val recomposer = Recomposer(Dispatchers.Default)
    val composition = ControlledComposition(
        applier = StringHtmlApplier(StringHtmlNodeWrapper(root)),
        parent = recomposer,
    )

    return try {
        composition.setContent {
            CompositionLocalProvider(
                LocalComposeHtmlContext provides StringComposeHtmlContext
            ) {
                content()
            }
        }
        root.toHtmlString()
    } finally {
        composition.dispose()
        recomposer.close()
    }
}
