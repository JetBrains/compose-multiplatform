/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.Text

// Compare browser-owned strings on their native side. Kotlin only needs copies for the uncommon
// mismatch diagnostics, which avoids repeated JS-to-Wasm string conversion during hydration.
internal expect fun Element.hasHtmlNamespace(): Boolean
internal expect fun Element.matchesElement(localName: String, namespace: String): Boolean
internal expect fun Text.matchesText(value: String): Boolean
internal expect fun Node.isRawTextContainer(): Boolean
