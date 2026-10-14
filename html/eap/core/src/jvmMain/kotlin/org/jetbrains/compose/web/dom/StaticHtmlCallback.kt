/* Copyright 2026 JetBrains s.r.o. Licensed under the Apache License, Version 2.0. */
@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)
package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.AttrsScope

// Keep the start tag in the callback so SSR can write it directly.
private class JvmPrecomputedHtmlCallback<TElement : Element>(
    val attributes: PrecomputedHtmlAttributes<TElement>,
) : Function1<AttrsScope<TElement>, Unit> {
    override fun invoke(target: AttrsScope<TElement>) = attributes.applyTo(target)
}

internal actual fun <TElement : Element> createPrecomputedHtmlCallback(
    attributes: PrecomputedHtmlAttributes<TElement>,
): AttrsScope<TElement>.() -> Unit = JvmPrecomputedHtmlCallback(attributes)

internal actual fun <TElement : Element> precomputedHtmlCallbackMetadata(
    callback: (AttrsScope<TElement>.() -> Unit)?,
): PrecomputedHtmlAttributes<TElement>? = (callback as? JvmPrecomputedHtmlCallback<TElement>)?.attributes
