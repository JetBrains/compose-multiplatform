/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Supplies browser DOM fixtures for JS.
package kotlinx.browser.dom.defaultarguments

import kotlinx.browser.document
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.Node

internal actual fun newDetachedNode(): Node = document.createTextNode("")

internal actual fun newDetachedElement(): Element = document.createElement("div")
