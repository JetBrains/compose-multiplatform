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

private const val HtmlDoctype = "<!doctype html>"

/**
 * Renders one complete HTML document that may contain browser-hydrated content.
 *
 * [content] must produce exactly one `html` element. The returned string always starts with an
 * HTML doctype. Static documents emit no hydration state. The application is responsible for
 * loading the client code that calls `hydrateRoot`.
 * Snapshot state changes made while rendering are discarded afterwards.
 * On the JVM or JS/Node, `COMPOSE_HTML_VALIDATE_STRICTLY=true` enables strict validation and records that
 * choice in each hydration state element. Pass `validateStrictly` to override the default.
 * Without strict validation, browser hydration trusts initial server values.
 *
 * That client code must run only after the [HydrationRoot] and its state element have been parsed.
 * Place its script after [HydrationRoot], defer an external classic script, or use a module script.
 */
fun renderHydratedDocument(
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    content: @Composable () -> Unit,
): String {
    val seenHydrationIds = if (validateStrictly) mutableSetOf<String>() else null
    return HtmlDoctype + composeHtmlString(requireHtmlDocumentRoot = true) {
        CompositionLocalProvider(
            LocalHydratedDocumentContext provides true,
            LocalHydrationIds provides seenHydrationIds,
            LocalHtmlValidationMode provides htmlValidationMode(validateStrictly),
        ) {
            content()
        }
    }
}

/**
 * Emits one browser-hydrated region of a document and its public initial state.
 *
 * [initialState] is serialized exactly once before [content] is composed. The serialized value is
 * public, readable in page source, and editable by the client. Always transfer a dedicated public
 * DTO. Never include credentials, private server models, or other sensitive values.
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
    hydrationId: String? = null,
    content: @Composable ElementScope<HTMLDivElement>.(T) -> Unit,
) {
    check(LocalHydratedDocumentContext.current) {
        "HydrationRoot must be called inside renderHydratedDocument"
    }
    require(hydrationId == null || hydrationId.isNotBlank()) {
        "hydrationId must not be blank"
    }
    val validationMode = LocalHtmlValidationMode.current
    val serializedState = serializeState(initialState)
    val seenHydrationIds = LocalHydrationIds.current

    Div(attrs = {
        rootAttrs?.invoke(this)
        hydrationProtocolAttr(HydrationRootAttribute, hydrationId.orEmpty())

        if (hydrationId != null && seenHydrationIds != null) { // only in strict mode
            require(seenHydrationIds.add(hydrationId)) {
                "Duplicate Compose hydrationId \"$hydrationId\""
            }
        }
    }) {
        content(initialState)
    }
    Script(
        content = InlineScript(serializedState.escapeForHydrationStateElement()),
        attrs = {
            hydrationProtocolAttr(HydrationStateAttribute, HydrationStateFormat)
            if (hydrationId != null) {
                hydrationProtocolAttr(HydrationForAttribute, hydrationId)
            }
            if (validationMode == HtmlValidationMode.Strict) {
                hydrationProtocolAttr(HydrationValidationAttribute, HydrationValidationEnabled)
            }
            type(ScriptType.TextPlain)
        },
    )
}

private val LocalHydratedDocumentContext = staticCompositionLocalOf { false }
private val LocalHydrationIds = staticCompositionLocalOf<MutableSet<String>?> { null }
