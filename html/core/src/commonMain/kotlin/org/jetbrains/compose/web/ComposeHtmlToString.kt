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
 * The result can contain internal comments that preserve ambiguous text-node boundaries for
 * hydration. They have no effect on the rendered content.
 *
 * Set [hydratable] to `false` for static output without hydration boundary comments.
 *
 * Known limitations:
 * - DOM property updates registered with `AttrsScope.prop(...)` are ignored because
 *   string rendering has no underlying DOM element.
 */
fun composeHtmlToString(
    hydratable: Boolean = true,
    content: @Composable () -> Unit,
): String = composeHtmlTree(content) { tree ->
    tree.toHtmlString(hydratable)
}

internal fun <T> composeHtmlTree(
    content: @Composable () -> Unit,
    readTree: (StringHtmlElementNode) -> T,
): T {
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
        readTree(root)
    } finally {
        composition.dispose()
        recomposer.close()
    }
}
