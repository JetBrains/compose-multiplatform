/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)
package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.AttrsScope

// Keep the start tag in the callback so SSR can write it directly.
private class JvmPreparedHtmlCallback<TElement : Element>(
    val attributes: PreparedHtmlAttributes<TElement>,
) : Function1<AttrsScope<TElement>, Unit> {
    override fun invoke(target: AttrsScope<TElement>) = attributes.applyTo(target)
}

internal actual fun <TElement : Element> createPreparedHtmlCallback(
    attributes: PreparedHtmlAttributes<TElement>,
): AttrsScope<TElement>.() -> Unit = JvmPreparedHtmlCallback(attributes)

internal actual fun <TElement : Element> preparedHtmlCallbackMetadata(
    callback: (AttrsScope<TElement>.() -> Unit)?,
): PreparedHtmlAttributes<TElement>? = (callback as? JvmPreparedHtmlCallback<TElement>)?.attributes
