/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.web.LocalSuspenseRegistry
import org.jetbrains.compose.web.SuspenseRegistry
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.SuspenseMarker

/** Composes child content before the parent decides what to show. */
internal class RetainedStringComposition(
    parent: CompositionContext,
    onChanges: () -> Unit,
) : RememberObserver {
    val root = StringHtmlElementNode.root()
    val composition = ControlledComposition(StringHtmlApplier(StringHtmlNodeWrapper(root), onChanges), parent)

    private val contentState = mutableStateOf<(@Composable () -> Unit)?>(null)
    private var initialized = false

    fun setContent(content: @Composable () -> Unit) {
        contentState.value = content
        if (!initialized) {
            composition.setContent {
                CompositionLocalProvider(LocalComposeHtmlContext provides StringComposeHtmlContext) {
                    checkNotNull(contentState.value).invoke()
                }
            }
            initialized = true
        }
    }

    override fun onRemembered() = Unit
    override fun onForgotten() = dispose()
    override fun onAbandoned() = dispose()
    private fun dispose() {
        composition.dispose()
    }
}

internal class StringHtmlSuspenseNode(
    val content: StringHtmlElementNode,
    var fallback: StringHtmlElementNode? = null,
    var pending: Boolean = false,
) : StringHtmlNode {
    var streamId: String? = null
    var parentTagName: String? = null
    var parentNamespace: String? = null
    val visible: StringHtmlElementNode get() = if (pending) checkNotNull(fallback) else content
    override fun appendHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        builder.appendSuspenseMarker(hydratable, streamId)
        visible.appendHtmlTo(builder, hydratable)
        builder.appendSuspenseMarker(hydratable, streamId, end = true)
    }
}

internal val LocalStringHtmlChanges = staticCompositionLocalOf<() -> Unit> { {} }

@OptIn(ComposeWebInternalApi::class)
internal fun StringBuilder.appendSuspenseMarker(hydratable: Boolean, id: String? = null, end: Boolean = false) {
    if (!hydratable && id == null) return
    append("<!--")
    if (end) append('/')
    append(SuspenseMarker)
    id?.let { append(':').append(it) }
    append("-->")
}

@Composable
internal fun stringSuspense(
    writer: HtmlStringWriterContext? = null,
    fallback: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val parent = rememberCompositionContext()
    val onChanges = LocalStringHtmlChanges.current
    val registry = remember { SuspenseRegistry() }
    val retained = remember { RetainedStringComposition(parent, onChanges) }
    retained.setContent {
        CompositionLocalProvider(LocalSuspenseRegistry provides registry) { content() }
    }
    val pending = registry.count != 0
    val node = remember { StringHtmlSuspenseNode(retained.root) }
    val fallbackRoot = if (pending) {
        val retainedFallback = remember { RetainedStringComposition(parent, onChanges) }
        retainedFallback.setContent(fallback)
        retainedFallback.root
    } else null
    if (writer != null) {
        writer.writeSuspense(fallbackRoot ?: retained.root)
    } else {
        ComposeNode<StringHtmlNodeWrapper, StringHtmlApplier>(
            factory = { StringHtmlNodeWrapper(node) },
            update = {
                set(pending) { (this.node as StringHtmlSuspenseNode).pending = it }
                set(fallbackRoot) { (this.node as StringHtmlSuspenseNode).fallback = it }
            },
        )
    }
}
