/* Copyright 2026 JetBrains s.r.o. Licensed under the Apache License, Version 2.0. */
@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

/** Generated target for literal HTML attributes. */
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

/** Forward without adding a composition group. */
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

@ComposeWebInternalApi
class PrecomputedHtmlText
internal constructor(internal val plain: String, internal val encoded: String)

@ComposeWebInternalApi
fun precomputedHtmlText(plain: String, encoded: String): PrecomputedHtmlText {
    require(StringBuilder().apply { appendEscapedText(plain) }.toString() == encoded)
    return PrecomputedHtmlText(plain, encoded)
}

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

/** Evaluate text after the start tag, inside its element scope. */
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

/** Generated target for a constant subtree with one HTML root. */
@ComposeWebInternalApi
class PrecomputedHtmlFragment
internal constructor(
    internal val rootTag: String,
    internal val encoded: String,
    internal val content: @Composable () -> Unit,
)

@ComposeWebInternalApi
fun precomputedHtmlFragment(
    rootTag: String,
    encoded: String,
    content: @Composable () -> Unit,
): PrecomputedHtmlFragment {
    require(rootTag in setOf("div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6"))
    // Render the original subtree once to check the precomputed HTML.
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

@ComposeWebInternalApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
fun LoweredStaticHtmlFragment(fragment: PrecomputedHtmlFragment) {
    LocalComposeHtmlContext.current.StaticHtmlFragment(fragment)
}

/** Skip wrapper calls while preserving the content lambda's scope. */
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
