/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

// Runtime support for generated code and tests.

/** Stores prepared HTML attributes. Created by [precomputedHtmlAttributes]. */
@ComposeWebInternalApi
class PrecomputedHtmlAttributes<TElement : Element>
internal constructor(
    internal val tagName: String,
    private val frozen: AttrsScopeBuilder<TElement>,
    internal val startTag: String,
) {
    internal val elementBuilder = createPlatformElementBuilder<TElement>(tagName)
    val callback: AttrsScope<TElement>.() -> Unit = createPrecomputedHtmlCallback(this)

    internal fun applyTo(target: AttrsScope<TElement>) {
        frozen.collect().forEach { (name, value) -> target.attr(name, value) }
        target.classes(frozen.classes)
    }
}

internal expect fun <TElement : Element> createPrecomputedHtmlCallback(
    attributes: PrecomputedHtmlAttributes<TElement>
): AttrsScope<TElement>.() -> Unit

internal expect fun <TElement : Element> precomputedHtmlCallbackMetadata(
    callback: (AttrsScope<TElement>.() -> Unit)?
): PrecomputedHtmlAttributes<TElement>?

/**
 * Builds & validates static, reusable attributes. TODO for compiler plugin
 * ```
 * private val attributes = precomputedHtmlAttributes<HTMLDivElement>(
 *     "div", "<div class=\"card\">",
 * ) {
 *     classes("card")
 * }
 *
  * fun Page(name: String) {
 *     LoweredHtmlContainer(attributes) {
 *         Text(name)
 *     }
 * }
* ```
 */
@OptIn(ComposeWebInternalApi::class)
@ComposeWebInternalApi
fun <TElement : Element> precomputedHtmlAttributes(
    tagName: String,
    expectedStartTag: String,
    initialize: AttrsScope<TElement>.() -> Unit,
): PrecomputedHtmlAttributes<TElement> {
    requireValidHtmlTagName(tagName)
    require(
        !isHtmlRawTextElement(tagName, HtmlNamespace) &&
            !isHtmlVoidElement(tagName, HtmlNamespace) &&
            tagName != "noscript"
    )
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
    require(encoded == expectedStartTag) { "Invalid precomputed HTML attributes" }
    return PrecomputedHtmlAttributes(tagName, attrs, expectedStartTag)
}

/** Renders a text-only element with prepared attributes. */
@OptIn(ComposeWebInternalApi::class)
@ComposeWebInternalApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
fun <TElement : Element> LoweredTextTag(
    attributes: PrecomputedHtmlAttributes<TElement>,
    value: String,
) {
    LocalComposeHtmlContext.current.TextTagElement(
        attributes.elementBuilder,
        attributes.callback,
        value,
        null,
    )
}

/** Stores plain and escaped literal text. Created by [precomputedHtmlText]. */
@ComposeWebInternalApi
class PrecomputedHtmlText
internal constructor(internal val plain: String, internal val encoded: String)

/** Validates and stores escaped literal text during initialization. */
@ComposeWebInternalApi
fun precomputedHtmlText(plain: String, encoded: String): PrecomputedHtmlText {
    require(StringBuilder().apply { appendEscapedText(plain) }.toString() == encoded)
    return PrecomputedHtmlText(plain, encoded)
}

/** Renders an element with prepared attributes and literal text. */
@OptIn(ComposeWebInternalApi::class)
@ComposeWebInternalApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
fun <TElement : Element> LoweredTextTag(
    attributes: PrecomputedHtmlAttributes<TElement>,
    value: PrecomputedHtmlText,
) {
    LocalComposeHtmlContext.current.TextTagElement(
        attributes.elementBuilder,
        attributes.callback,
        value.plain,
        value.encoded,
    )
}

/** Evaluates dynamic text inside the prepared element's scope. */
@ComposeWebInternalApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
fun <TElement : Element> LoweredDynamicTextTag(
    attributes: PrecomputedHtmlAttributes<TElement>,
    value: @Composable ElementScope<TElement>.() -> String,
) {
    LocalComposeHtmlContext.current.DynamicTextTagElement(attributes, value)
}

/**
 * Stores constant HTML, root-tag metadata, and the original browser composition lambda.
 * The IR compiler plugin emits constructor calls that execute at file initialization.
 * It checks the HTML and root tag before emitting those calls.
 */
@ComposeWebInternalApi
class PrecomputedHtmlFragment(
    internal val rootTag: String,
    internal val encoded: String,
    internal val content: @Composable () -> Unit,
)

/**
 * Used by tests to render [content] once and check it against [encoded] and [rootTag].
 */
@ComposeWebInternalApi
fun precomputedHtmlFragment(
    rootTag: String,
    encoded: String,
    content: @Composable () -> Unit,
): PrecomputedHtmlFragment {
    val validator = HtmlStringWriterContext(hydratable = true, fragmentRootTag = rootTag)
    org.jetbrains.compose.web.composeHtmlToString(validateStrictly = true) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalComposeHtmlContext provides validator
        ) {
            content()
        }
    }
    require(validator.finish(false) == encoded) { "Invalid precomputed HTML subtree" }
    return PrecomputedHtmlFragment(rootTag, encoded, content)
}

/**
 * Called by code emitted by the IR compiler plugin.
 * SSR writes stored HTML; browser composition invokes the original lambda.
 */
@ComposeWebInternalApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
fun LoweredStaticHtmlFragment(fragment: PrecomputedHtmlFragment) {
    LocalComposeHtmlContext.current.StaticHtmlFragment(fragment)
}

/**
 * Called by code emitted by the IR compiler plugin. TODO
 * Renders a prepared element while composing its children normally.
 */
@ComposeWebInternalApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
fun <TElement : Element> LoweredHtmlContainer(
    attributes: PrecomputedHtmlAttributes<TElement>,
    content: @Composable ElementScope<TElement>.() -> Unit,
) {
    LocalComposeHtmlContext.current.TagElement(
        attributes.elementBuilder,
        attributes.callback,
        null,
        content,
    )
}
