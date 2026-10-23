/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.HtmlValidationMode
import org.jetbrains.compose.web.LocalHtmlValidationMode
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

// Runtime support for generated code and tests.

/** Stores prepared HTML attributes. Created by [preparedHtmlAttributes]. */
@ComposeWebInternalApi
class PreparedHtmlAttributes<TElement : Element>
internal constructor(
    internal val tagName: String,
    private val frozen: AttrsScopeBuilder<TElement>,
    internal val startTag: String,
) {
    internal val elementBuilder = createPlatformElementBuilder<TElement>(tagName)
    val callback: AttrsScope<TElement>.() -> Unit = createPreparedHtmlCallback(this)

    internal fun applyTo(target: AttrsScope<TElement>) {
        frozen.collect().forEach { (name, value) -> target.attr(name, value) }
        target.classes(frozen.classes)
    }
}

internal expect fun <TElement : Element> createPreparedHtmlCallback(
    attributes: PreparedHtmlAttributes<TElement>
): AttrsScope<TElement>.() -> Unit

internal expect fun <TElement : Element> preparedHtmlCallbackMetadata(
    callback: (AttrsScope<TElement>.() -> Unit)?
): PreparedHtmlAttributes<TElement>?

/**
 * Only called by compiler plugin.
 * Builds and validates reusable attributes for generated code.
 * ```
 * private val attributes = preparedHtmlAttributes<HTMLDivElement>(
 *     "div", "<div class=\"card\">",
 * ) {
 *     classes("card")
 * }
 *
 * fun Page(name: String) {
 *     PreparedContainer(attributes) {
 *         Text(name)
 *     }
 * }
 * ```
 */
@ComposeWebInternalApi
fun <TElement : Element> preparedHtmlAttributes(
    tagName: String,
    expectedStartTag: String,
    initialize: AttrsScope<TElement>.() -> Unit,
): PreparedHtmlAttributes<TElement> {
    requireValidHtmlTagName(tagName)
    require(supportsPreparedHtmlAttributes(tagName))
    val attrs = AttrsScopeBuilder<TElement>().apply(initialize)
    require(
        attrs.propertyUpdatesOrEmpty.isEmpty() &&
            attrs.styleScopeOrNull == null &&
            attrs.refEffect == null &&
            attrs.hydrationProtocolAttributes.isEmpty() &&
            !attrs.allowsHydrationMismatch
    )
    val encoded =
        StringBuilder()
            .apply { appendStartTag(tagName, HtmlNamespace, attrs, true, null, null) }
            .toString()
    require(encoded == expectedStartTag) { "Invalid prepared HTML attributes" }
    return PreparedHtmlAttributes(tagName, attrs, expectedStartTag)
}

/** Only called by compiler plugin. Evaluates dynamic text inside the prepared element's scope. */
@ComposeWebInternalApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
fun <TElement : Element> PreparedTextElement(
    attributes: PreparedHtmlAttributes<TElement>,
    value: @Composable ElementScope<TElement>.() -> String,
) {
    LocalComposeHtmlContext.current.DynamicTextTagElement(attributes, value)
}

/**
 * Only called by compiler plugin.
 * Writes compiler-validated HTML directly to the current output buffer.
 * Inlining puts the StringBuilder append in the caller.
 */
@ComposeWebInternalApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
@Suppress("NOTHING_TO_INLINE") // Put the append in generated callers, without a callback allocation.
inline fun WriteStaticHtml(rootTag: String, encoded: String) {
    val writer = currentHtmlStringWriter()
    val output = writer.beginStaticHtml(rootTag, encoded, LocalHtmlValidationMode.current == HtmlValidationMode.Strict) ?: return
    output.append(encoded)
    writer.flushChunk()
}

/** Used by compiler-generated calls and inline [WriteStaticHtml]. */
@PublishedApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
internal fun currentHtmlStringWriter(): HtmlStringWriterContext =
    LocalComposeHtmlContext.current as? HtmlStringWriterContext
        ?: error("Direct HTML writes require HtmlStringWriterContext; this rendering context does not support them")

/** Only called by compiler plugin. */
@PublishedApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
@Suppress("NOTHING_TO_INLINE")
internal inline fun currentHtmlValidationIsStrict(): Boolean =
    LocalHtmlValidationMode.current == HtmlValidationMode.Strict

/**
 * Only called by compiler plugin.
 * Renders a prepared element while composing its children normally.
 */
@ComposeWebInternalApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
fun <TElement : Element> PreparedContainer(
    attributes: PreparedHtmlAttributes<TElement>,
    content: @Composable ElementScope<TElement>.() -> Unit,
) {
    LocalComposeHtmlContext.current.TagElement(
        attributes.elementBuilder,
        attributes.callback,
        null,
        content,
    )
}
