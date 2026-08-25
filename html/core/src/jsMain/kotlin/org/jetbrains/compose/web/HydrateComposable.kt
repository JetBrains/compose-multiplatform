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
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.dom.HydrationDomApplier
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext
import org.jetbrains.compose.web.dom.hydratingComposeHtmlContext
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.GlobalSnapshotManager
import org.jetbrains.compose.web.internal.runtime.JsMicrotasksDispatcher
import org.w3c.dom.Element

/**
 * Adopts an existing server-rendered DOM tree.
 */
@OptIn(ComposeWebInternalApi::class)
internal fun <TElement : Element> hydrateComposable(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
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
        applier.finishHydration()
    } catch (throwable: Throwable) {
        throwable.suppressCleanupFailure(applier::abortHydration)
        throwable.suppressCleanupFailure(composition::dispose)
        throwable.suppressCleanupFailure(recomposer::close)
        throw throwable
    }

    return composition
}

private inline fun Throwable.suppressCleanupFailure(cleanup: () -> Unit) {
    try {
        cleanup()
    } catch (cleanupFailure: Throwable) {
        addSuppressed(cleanupFailure)
    }
}
