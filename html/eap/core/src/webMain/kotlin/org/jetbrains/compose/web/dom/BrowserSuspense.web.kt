/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.web.LocalSuspenseRegistry
import org.jetbrains.compose.web.SuspenseRegistry
import org.jetbrains.compose.web.internal.runtime.DomApplier
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.DomRangeNodeWrapper

internal class SuspenseDomEffects(private val parent: SuspenseDomEffects?) {
    private val deferred = mutableListOf<() -> Unit>()
    private var visible = false
    private var disposed = false

    fun dispatch(effect: () -> Unit) {
        if (disposed) return
        if (!visible) deferred += effect
        else if (parent != null) parent.dispatch(effect)
        else effect()
    }

    fun hide() { visible = false }

    fun show() {
        visible = true
        val effects = deferred.toList()
        deferred.clear()
        effects.forEach(::dispatch)
    }

    fun dispose() {
        disposed = true
        deferred.clear()
    }
}

internal val LocalSuspenseDomEffects = staticCompositionLocalOf<SuspenseDomEffects?> { null }

private class BrowserSuspenseComposition(
    private val parent: CompositionContext,
    val registry: SuspenseRegistry,
    private val parentDomEffects: SuspenseDomEffects?,
) : RememberObserver {
    private val contentDomEffects = SuspenseDomEffects(parentDomEffects)
    private var fallbackDomEffects: SuspenseDomEffects? = null
    lateinit var range: DomRangeNodeWrapper
        private set
    private lateinit var retained: ControlledComposition
    private var fallbackComposition: ControlledComposition? = null
    private lateinit var contentRoot: DomRangeNodeWrapper
    private var fallbackRoot: DomRangeNodeWrapper? = null
    private var shown: DomRangeNodeWrapper? = null
    private var htmlContext: ComposeHtmlContext = DefaultComposeHtmlContext
    private var hydration: HydrationDomApplier? = null
    private var outerHydration: HydrationDomApplier? = null
    private val contentState = mutableStateOf<(@Composable () -> Unit)?>(null)
    private val fallbackState = mutableStateOf<(@Composable () -> Unit)?>(null)
    private var contentInitialized = false

    fun initialize(applier: HydrationDomApplier?) {
        range = if (applier?.isHydrating == true) applier.claimSuspenseRange() else DomRangeNodeWrapper()
        if (applier?.isHydrating == true) {
            // Completed SSR content is ready to hydrate. Pending fallbacks trigger mismatch recovery
            // and keep the client loader running.
            outerHydration = applier
            contentRoot = DomRangeNodeWrapper(range.start, range.end)
            val contentHydration = HydrationDomApplier(contentRoot, applier.validationMode, applier::applyOrDeferDomMutation)
            hydration = contentHydration
            applier.onAbortHydration { contentHydration.abortHydration() }
            htmlContext = hydratingComposeHtmlContext(contentHydration)
            retained = ControlledComposition(contentHydration, parent)
            shown = contentRoot
        } else {
            contentRoot = DomRangeNodeWrapper()
            retained = ControlledComposition(DomApplier(contentRoot), parent)
        }
    }

    fun content(content: @Composable () -> Unit) {
        contentState.value = content
        if (!contentInitialized) {
            retained.setContent {
                CompositionLocalProvider(
                    LocalComposeHtmlContext provides htmlContext,
                    LocalSuspenseRegistry provides registry,
                    LocalSuspenseDomEffects provides contentDomEffects,
                ) { checkNotNull(contentState.value).invoke() }
            }
            contentInitialized = true
            hydration?.let { if (it.isHydrating) it.finishHydration() }
            outerHydration?.let { outer ->
                outer.applyOrDeferDomMutation { contentRoot.wrapContentWithin(range) }
                outerHydration = null
            }
        }
    }

    fun fallback(content: @Composable () -> Unit) { fallbackState.value = content }

    fun reconcile() {
        val pending = registry.count != 0
        if (pending && fallbackComposition == null) {
            val root = DomRangeNodeWrapper()
            fallbackRoot = root
            fallbackDomEffects = SuspenseDomEffects(parentDomEffects)
            val composition = ControlledComposition(DomApplier(root), parent)
            fallbackComposition = composition
            composition.setContent {
                CompositionLocalProvider(
                    LocalComposeHtmlContext provides DefaultComposeHtmlContext,
                    LocalSuspenseDomEffects provides fallbackDomEffects,
                ) {
                    checkNotNull(fallbackState.value).invoke()
                }
            }
        }
        val next = if (pending) checkNotNull(fallbackRoot) else contentRoot
        if (pending) contentDomEffects.hide() else fallbackDomEffects?.hide()
        if (shown !== next) {
            if (shown != null) range.remove(0, 1)
            range.insert(0, next)
            shown = next
        }
        if (!pending) {
            fallbackComposition?.dispose()
            fallbackComposition = null
            fallbackRoot = null
            fallbackDomEffects?.dispose()
            fallbackDomEffects = null
        }
    }

    fun flushDomEffects() {
        if (shown === contentRoot) contentDomEffects.show() else fallbackDomEffects?.show()
    }

    override fun onRemembered() = Unit
    override fun onForgotten() = dispose()
    override fun onAbandoned() = dispose()
    private fun dispose() {
        contentDomEffects.dispose()
        fallbackDomEffects?.dispose()
        hydration?.abortHydration()
        try {
            fallbackComposition?.dispose()
        } finally {
            if (::retained.isInitialized) retained.dispose()
        }
    }
}

@Composable
internal fun browserSuspense(
    applier: HydrationDomApplier? = null,
    fallback: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val parent = rememberCompositionContext()
    val registry = remember { SuspenseRegistry() }
    val domEffects = LocalSuspenseDomEffects.current
    val retained = remember { BrowserSuspenseComposition(parent, registry, domEffects) }
    // Recompose the parent when pending calls change, including during child recomposition.
    registry.count
    ComposeNode<DomNodeWrapper, Applier<DomNodeWrapper>>(
        factory = {
            retained.initialize(applier)
            retained.range
        },
        update = {
            set(content) { retained.content(it) }
            set(fallback) { retained.fallback(it) }
            reconcile { retained.reconcile() }
        },
    )
    // Expose DOM references after the outer range is inserted.
    SideEffect { retained.flushDomEffects() }
}
