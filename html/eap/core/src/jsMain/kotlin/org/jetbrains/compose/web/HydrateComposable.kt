package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import kotlinx.browser.document
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.ParentNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.ElementScopeImpl
import org.jetbrains.compose.web.dom.HydrationDomApplier
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext
import org.jetbrains.compose.web.dom.hydratingComposeHtmlContext
import org.jetbrains.compose.web.internal.unsafeCast
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.GlobalSnapshotManager
import org.jetbrains.compose.web.internal.runtime.JsMicrotasksDispatcher
import org.w3c.dom.Element
import kotlin.js.console

/** Indicates that hydration state could not be located, validated, or deserialized. */
class HydrationStateException internal constructor(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

/**
 * Finds the root and initial state emitted by [HydrationRoot], deserializes the state, and adopts
 * the server-rendered DOM. [deserializeState] must match the serializer used on the server. Treat
 * the result as an immutable snapshot.
 *
 * The payload is untrusted, user-editable input. Use a safely configured deserializer, and validate
 * and authorize any values sent back to a server.
 *
 * The state element remains in the document after hydration. Invalid or missing state throws
 * [HydrationStateException] before composition starts, without invoking [onHydrationMismatch] or
 * modifying the server-rendered DOM.
 *
 * This function must run only after [HydrationRoot] and its state element have been parsed. Place
 * the bootstrap script after [HydrationRoot], defer an external classic script, or use a module
 * script. By default [within] is the browser document. Pass a narrower container to restrict
 * protocol-element discovery to that subtree.
 *
 * The returned [Composition] owns the hydrated application and can be disposed when the
 * application is no longer needed.
 *
 * @throws HydrationStateException if the protocol elements or serialized state are invalid.
 */
fun <T> hydrateRoot(
    deserializeState: (String) -> T,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    onHydrationMismatch: (HydrationMismatchException) -> Unit = { console.error(it) },
    within: ParentNode = document,
    content: @Composable ElementScope<HTMLDivElement>.(T) -> Unit,
): Composition {
    val protocol = findHydrationProtocol(within)
    val initialState = try {
        deserializeState(protocol.serializedState)
    } catch (failure: Throwable) {
        invalidHydrationState(
            "Failed to deserialize the Compose hydration state",
            failure,
        )
    }
    val scope = ElementScopeImpl<HTMLDivElement>().apply {
        element = protocol.root
    }

    return hydrateComposable(
        root = protocol.root,
        monotonicFrameClock = monotonicFrameClock,
        onHydrationMismatch = onHydrationMismatch,
    ) {
        content(scope, initialState)
    }
}

/**
 * Adopts an existing server-rendered DOM tree. If it does not match the composition, reports the
 * first mismatch and falls back to a client render. Formatting-only HTML whitespace immediately
 * inside [root], before or after the composed content, is ignored. A throwing
 * [onHydrationMismatch] aborts the fallback and leaves the server DOM untouched.
 *
 * Content that the client cannot reproduce, such as a server timestamp, can keep its
 * server-rendered element by opting out of the comparison with
 * [org.jetbrains.compose.web.attributes.AttrsScope.allowHydrationMismatch].
 */
@OptIn(ComposeWebInternalApi::class)
fun <TElement : Element> hydrateComposable(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    onHydrationMismatch: (HydrationMismatchException) -> Unit = { console.error(it) },
    content: @Composable DOMScope<TElement>.() -> Unit,
): Composition = try {
    hydrateOnce(
        root = root,
        monotonicFrameClock = monotonicFrameClock,
        content = content,
    )
} catch (mismatch: HydrationMismatchException) {
    onHydrationMismatch(mismatch)
    root.clear()
    renderComposable(
        root = root,
        monotonicFrameClock = monotonicFrameClock,
        content = content,
    )
}

@OptIn(ComposeWebInternalApi::class)
private fun <TElement : Element> hydrateOnce(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit,
): Composition {
    GlobalSnapshotManager.ensureStarted()

    val coroutineContext = monotonicFrameClock + JsMicrotasksDispatcher()
    val recomposer = Recomposer(coroutineContext)
    CoroutineScope(coroutineContext).launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    val applier = HydrationDomApplier(DomNodeWrapper(root))
    val composition = ControlledComposition(
        applier = applier,
        parent = recomposer,
    )
    val htmlContext = hydratingComposeHtmlContext(applier)
    val scope = object : DOMScope<TElement> {
        override val DisposableEffectScope.scopeElement: TElement
            get() = root
    }

    try {
        composition.setContent {
            CompositionLocalProvider(
                LocalComposeHtmlContext provides htmlContext,
            ) {
                content(scope)
            }
        }
        // Initial DOM changes normally finish hydration from Applier.onEndChanges(), before
        // remember observers and effects run. An empty composition can produce no change batch.
        if (applier.isHydrating) {
            applier.finishHydration()
        }
        return composition
    } catch (throwable: Throwable) {
        throwable.suppressCleanupFailure(applier::abortHydration)
        throwable.suppressCleanupFailure(composition::dispose)
        throwable.suppressCleanupFailure(recomposer::close)
        throw throwable
    }
}

private inline fun Throwable.suppressCleanupFailure(cleanup: () -> Unit) {
    try {
        cleanup()
    } catch (cleanupFailure: Throwable) {
        addSuppressed(cleanupFailure)
    }
}

private fun findHydrationProtocol(within: ParentNode): HydrationProtocol {
    val rootElement = findUniqueProtocolElement(
        within = within,
        selector = "[$HydrationRootAttribute]",
        description = "hydration root",
    )
    if (!rootElement.tagName.equals("div", ignoreCase = true)) {
        invalidHydrationState("The Compose hydration root must be a <div>")
    }
    val root = rootElement.unsafeCast<HTMLDivElement>()

    val state = findUniqueProtocolElement(
        within = within,
        selector = "[$HydrationStateAttribute]",
        description = "hydration state element",
    )
    if (root.contains(state)) {
        invalidHydrationState(
            "The Compose hydration state element must be outside the hydration root"
        )
    }
    if (!state.tagName.equals("script", ignoreCase = true)) {
        invalidHydrationState("The Compose hydration state element must be a <script>")
    }
    val mimeType = state.getAttribute("type")?.substringBefore(';')?.trim()
    if (!mimeType.equals(HydrationStateMimeType, ignoreCase = true)) {
        invalidHydrationState(
            "The Compose hydration state element must have type \"$HydrationStateMimeType\""
        )
    }
    val format = state.getAttribute(HydrationStateAttribute)
    if (format != HydrationStateFormat) {
        invalidHydrationState("The Compose hydration state has unsupported format \"$format\"")
    }

    return HydrationProtocol(
        root = root,
        serializedState = state.textContent.orEmpty().unescapeFromHydrationStateElement(),
    )
}

private fun findUniqueProtocolElement(
    within: ParentNode,
    selector: String,
    description: String,
): Element {
    val matches = within.querySelectorAll(selector)
    if (matches.length != 1) {
        val timingHint = if (matches.length == 0) {
            " Make sure hydrateRoot runs after the hydration root and state have been parsed; " +
                "defer its bootstrap script or move it to the end of <body>."
        } else {
            ""
        }
        invalidHydrationState(
            "Expected exactly one Compose $description, but found ${matches.length}." + timingHint,
        )
    }
    return matches.item(0) as? Element
        ?: invalidHydrationState("The Compose $description is not an element")
}

private fun invalidHydrationState(message: String, cause: Throwable? = null): Nothing =
    throw HydrationStateException(message, cause)

private class HydrationProtocol(
    val root: HTMLDivElement,
    val serializedState: String,
)
