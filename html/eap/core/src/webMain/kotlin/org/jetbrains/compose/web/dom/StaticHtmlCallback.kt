/* Copyright 2026 JetBrains s.r.o. Licensed under the Apache License, Version 2.0. */
@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)
package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.AttrsScope

internal actual fun <TElement : Element> createPrecomputedHtmlCallback(
    attributes: PrecomputedHtmlAttributes<TElement>,
): AttrsScope<TElement>.() -> Unit = { attributes.applyTo(this) }

internal actual fun <TElement : Element> precomputedHtmlCallbackMetadata(
    callback: (AttrsScope<TElement>.() -> Unit)?,
): PrecomputedHtmlAttributes<TElement>? = null
