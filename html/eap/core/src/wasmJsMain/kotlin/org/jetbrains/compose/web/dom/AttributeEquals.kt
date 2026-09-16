/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

// Perform the common exact comparison before crossing into Kotlin/Wasm. Mismatches still use the
// typed Kotlin path so normalization, diagnostics, and mismatch allowances retain their behavior.
@JsFun("(node, name, expected) => node.namespaceURI === 'http://www.w3.org/1999/xhtml' && node.getAttribute(name) === expected")
private external fun htmlAttributeEquals(node: Element, name: String, expected: String?): Boolean

internal actual fun Element.matchesHtmlAttribute(name: String, expected: String?): Boolean =
    htmlAttributeEquals(this, name, expected)
