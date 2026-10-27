/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.AttrsScope

internal actual fun <TElement : Element> createPreparedHtmlCallback(
    attributes: PreparedHtmlAttributes<TElement>,
): AttrsScope<TElement>.() -> Unit = { attributes.applyTo(this) }

internal actual fun <TElement : Element> preparedHtmlCallbackMetadata(
    callback: (AttrsScope<TElement>.() -> Unit)?,
): PreparedHtmlAttributes<TElement>? = null
