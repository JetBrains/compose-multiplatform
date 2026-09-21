/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.Text

// Keep successful DOM comparisons in JavaScript; Kotlin handles diagnostics.
@JsFun("node => node.namespaceURI === 'http://www.w3.org/1999/xhtml'")
private external fun hasHtmlNamespace(node: Element): Boolean

@JsFun("(node, name, namespace) => node.localName === name && node.namespaceURI === namespace")
private external fun matchesElement(node: Element, name: String, namespace: String): Boolean

@JsFun("(node, value) => node.data === value")
private external fun matchesText(node: Text, value: String): Boolean

@JsFun(
    "node => node.namespaceURI === 'http://www.w3.org/1999/xhtml' && " +
        "['script', 'style', 'iframe', 'xmp', 'noembed', 'noframes'].includes(node.localName)"
)
private external fun isRawTextContainer(node: Node): Boolean

internal actual fun Element.hasHtmlNamespace(): Boolean = hasHtmlNamespace(this)
internal actual fun Element.matchesElement(localName: String, namespace: String): Boolean =
    matchesElement(this, localName, namespace)
internal actual fun Text.matchesText(value: String): Boolean = matchesText(this, value)
internal actual fun Node.isRawTextContainer(): Boolean = isRawTextContainer(this)
