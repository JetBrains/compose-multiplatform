package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.dom.HydrationDomApplier
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext
import org.jetbrains.compose.web.dom.hydratingComposeHtmlContext
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.GlobalSnapshotManager
import org.jetbrains.compose.web.internal.runtime.JsMicrotasksDispatcher
import org.w3c.dom.Element
import kotlin.js.console

/**
 * Deserializes data emitted by the matching [composeHtmlToString] overload and uses it for the
 * initial composition. [deserializeData] must match the server's serializer. Treat the result as
 * an immutable snapshot.
 *
 * The payload is untrusted, user-editable input. Use a safely configured deserializer, and validate
 * and authorize any values sent back to a server.
 *
 * The data element remains in the document after hydration. Invalid or missing data throws
 * [HydrationDataException] before composition starts, without invoking [onHydrationMismatch] or
 * modifying the server-rendered DOM.
 *
 * [hydrationDataId] must be unique in the document. Use a different id for each hydration root.
 *
 * @throws IllegalArgumentException if [hydrationDataId] is not a supported hydration data id.
 */
fun <TElement : Element, T> hydrateComposable(
    root: TElement,
    deserializeData: (String) -> T,
    hydrationDataId: String = DEFAULT_HYDRATION_DATA_ID,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    onHydrationMismatch: (HydrationMismatchException) -> Unit = { console.error(it) },
    content: @Composable DOMScope<TElement>.(T) -> Unit,
): Composition {
    val serializedData = findHydrationData(root, hydrationDataId)
    val data = try {
        deserializeData(serializedData)
    } catch (failure: Throwable) {
        throw HydrationDataException(
            "Failed to deserialize hydration data from element \"$hydrationDataId\"",
            failure,
        )
    }

    return hydrateComposable(
        root = root,
        monotonicFrameClock = monotonicFrameClock,
        onHydrationMismatch = onHydrationMismatch,
    ) {
        content(data)
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

private fun findHydrationData(root: Element, hydrationDataId: String): String {
    requireValidHydrationDataId(hydrationDataId)
    fun invalid(message: String): Nothing = throw HydrationDataException(message)

    val ownerDocument = root.ownerDocument
        ?: invalid("The hydration root has no owner document")
    val matches = ownerDocument.querySelectorAll("#$hydrationDataId")
    val element = when (matches.length) {
        0 -> invalid(
            "No hydration data element with id \"$hydrationDataId\" was found",
        )
        1 -> matches.item(0) as? Element
            ?: invalid(
                "Hydration data node \"$hydrationDataId\" is not an element",
            )
        else -> invalid(
            "Expected one hydration data element with id \"$hydrationDataId\", " +
                "but found ${matches.length}",
        )
    }

    val description = "Hydration data element \"$hydrationDataId\""
    if (root.contains(element)) {
        invalid("$description must be outside the hydration root")
    }
    if (!element.tagName.equals("script", ignoreCase = true)) {
        invalid("$description must be a <script>")
    }
    val mimeType = element.getAttribute("type")?.substringBefore(';')?.trim()
    if (!mimeType.equals(HydrationDataMimeType, ignoreCase = true)) {
        invalid("$description must have type \"$HydrationDataMimeType\"")
    }
    val format = element.getAttribute(HydrationDataAttribute)
    if (format != HydrationDataFormat) {
        invalid("$description has unsupported format \"$format\"")
    }

    return element.textContent.orEmpty().unescapeFromHydrationDataElement()
}
