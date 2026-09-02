package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.dom.HTMLDivElement
import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.InlineScript
import org.jetbrains.compose.web.dom.Script
import org.jetbrains.compose.web.dom.StringHtmlElementNode
import org.jetbrains.compose.web.dom.StringHtmlNode
import org.jetbrains.compose.web.dom.isEmptyText

private const val HtmlDoctype = "<!doctype html>"

/**
 * Renders one complete HTML document that may contain browser-hydrated content.
 *
 * [content] must produce exactly one `html` element. The returned string always starts with an
 * HTML doctype. Static documents emit no hydration state. The application is responsible for
 * loading the client code that calls `hydrateRoot`.
 *
 * That client code must run only after the [HydrationRoot] and its state element have been parsed.
 * Place its script after [HydrationRoot], defer an external classic script, or use a module script.
 */
fun renderHydratedDocument(
    content: @Composable () -> Unit,
): String = composeHtmlTree(
    content = {
        CompositionLocalProvider(LocalHydratedDocumentContext provides true) {
            content()
        }
    },
    readTree = { tree ->
        validateHydratedDocument(tree)
        HtmlDoctype + tree.toHtmlString(hydratable = true)
    },
)

/**
 * Emits one browser-hydrated region of a document and its public initial state.
 *
 * [initialState] is serialized exactly once before [content] is composed. The serialized value is
 * public, readable in page source, and editable by the client. Always transfer a dedicated public
 * DTO; never include credentials, private server models, or other sensitive values.
 *
 * The payload is emitted in an inert `script[type=text/plain]`. It contains no executable inline
 * script and therefore does not require a Content Security Policy nonce.
 *
 * [rootAttrs] are written only to the server-rendered root. Hydration adopts the root's children,
 * so it neither compares nor updates attributes on the root itself. Use [rootAttrs] only for values
 * that do not need client-side reconciliation.
 *
 * DOM references and element effects used through the content receiver are available after
 * browser hydration, but throw during server string rendering.
 */
@Composable
fun <T> HydrationRoot(
    initialState: T,
    serializeState: (T) -> String,
    rootAttrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: @Composable ElementScope<HTMLDivElement>.(T) -> Unit,
) {
    check(LocalHydratedDocumentContext.current) {
        "HydrationRoot must be called inside renderHydratedDocument"
    }
    val serializedState = serializeState(initialState)

    Div(attrs = {
        rootAttrs?.invoke(this)
        hydrationProtocolAttr(HydrationRootAttribute, "")
    }) {
        content(initialState)
    }
    Script(
        content = InlineScript(serializedState.escapeForHydrationStateElement()),
        attrs = {
            hydrationProtocolAttr(HydrationStateAttribute, HydrationStateFormat)
            type(ScriptType.TextPlain)
        },
    )
}

private val LocalHydratedDocumentContext = staticCompositionLocalOf { false }

private fun validateHydratedDocument(tree: StringHtmlElementNode) {
    val documentChildren = tree.children.filterNot(StringHtmlNode::isEmptyText)
    val html = documentChildren.singleOrNull() as? StringHtmlElementNode
    require(html?.tagName == "html") {
        "renderHydratedDocument content must produce exactly one html element"
    }
}
