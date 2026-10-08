/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

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
 * Renders a complete HTML document with optional browser hydration.
 *
 * [content] must produce exactly one `html` element. Output starts with an HTML doctype.
 * Static documents emit no hydration state.
 *
 * Load client code that calls `hydrateRoot` after [HydrationRoot] and its state element are parsed:
 * place the script after them, defer an external classic script, or use a module script.
 *
 * Snapshot state changes are discarded after rendering.
 *
 * On the JVM or Node.js, `COMPOSE_HTML_VALIDATE_STRICTLY=true` enables strict validation.
 * [validateStrictly] overrides it.
 * Without strict validation, browser hydration trusts initial server values.
 *
 * Calls are thread-safe. Synchronize shared mutable data accessed by [content].
 * See [composeHtmlToStream] for validation and failure handling.
 *
 * Use [renderHydratedDocumentToStream] for incremental output.
 *
 * @param key stable key per template for best-effort storage reuse. `null` disables pooling.
 */
fun renderHydratedDocument(
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    key: String?,
    content: @Composable () -> Unit,
): String = composeHydratedDocument(validateStrictly = validateStrictly, key = key, content = content)

/**
 * Renders a complete document with fresh storage.
 *
 * Calls are thread-safe. Callers must Synchronize shared mutable data accessed by [content].
 * See [composeHtmlToStream] for validation and failure handling.
 */
fun renderHydratedDocument(
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    content: @Composable () -> Unit,
): String = renderHydratedDocument(validateStrictly = validateStrictly, key = null, content = content)

/**
 * Streams the same HTML as [renderHydratedDocument], including its doctype and hydration state.
 * [content] must produce exactly one `html` element; client loading requirements follow
 * [renderHydratedDocument].
 *
 * [chunkSize] defaults to 2048 UTF-16 code units. Tags and `Text` calls stay intact, so chunks
 * may exceed the target. [sink] runs synchronously, may block, and receives no empty chunks.
 * Emitted output cannot be retracted on failure, including document validation after composition.
 *
 * Calls are thread-safe. Callers must Synchronize shared mutable data accessed by [content].
 * See [composeHtmlToStream] for validation and failure handling.
 *
 * @param key when non-null, enables pooled rendering storage across calls and threads.
 * @param validateStrictly overrides validation and records the choice in hydration state.
 * @throws IllegalArgumentException if [chunkSize] is not positive or [content] is not a valid document.
 */
fun renderHydratedDocumentToStream(
    sink: (String) -> Unit,
    chunkSize: Int = 2048,
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    key: String?,
    content: @Composable () -> Unit,
) {
    composeHydratedDocument(
        validateStrictly, key = key, chunkSink = { sink(it.toString()) }, chunkSize = chunkSize, content = content,
    )
}

/**
 * Streams a complete document with fresh storage.
 *
 * Calls are thread-safe. Callers must Synchronize shared mutable data accessed by [content].
 * See [composeHtmlToStream] for validation and failure handling.
 */
fun renderHydratedDocumentToStream(
    sink: (String) -> Unit,
    chunkSize: Int = 2048,
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    content: @Composable () -> Unit,
) {
    renderHydratedDocumentToStream(sink, chunkSize, validateStrictly, key = null, content = content)
}

/** Shared document context and protocol for complete-string and streaming output. */
internal fun composeHydratedDocument(
    validateStrictly: Boolean,
    chunkSink: ((StringBuilder) -> Unit)? = null,
    chunkSize: Int = 2048,
    key: String? = null,
    content: @Composable () -> Unit,
): String {
    val seenHydrationIds = if (validateStrictly) mutableSetOf<String>() else null
    return composeHtmlString(
        requireHtmlDocumentRoot = true,
        chunkSink = chunkSink,
        chunkSize = chunkSize,
        prefix = HtmlDoctype,
        key = key,
    ) {
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
 * browser hydration, but throw during server rendering.
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
        "HydrationRoot must be called inside renderHydratedDocument or renderHydratedDocumentToStream"
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
