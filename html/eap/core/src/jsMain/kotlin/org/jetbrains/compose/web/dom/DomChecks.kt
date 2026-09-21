/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.Text

// Compare browser strings directly without allocations.
internal actual fun Element.hasHtmlNamespace(): Boolean = namespaceURI == HtmlNamespace
internal actual fun Element.matchesElement(localName: String, namespace: String): Boolean =
    this.localName == localName && namespaceURI == namespace
internal actual fun Text.matchesText(value: String): Boolean = data == value
internal actual fun Node.isRawTextContainer(): Boolean =
    this is Element && isHtmlRawTextElement(localName, namespaceURI)
