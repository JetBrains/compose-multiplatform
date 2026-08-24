/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Supplies JVM stubs for generated default arguments.
package kotlinx.browser.dom.defaultarguments

import kotlinx.browser.dom.Element
import kotlinx.browser.dom.Node

private class DetachedNode : Node()

private class DetachedElement : Element()

internal actual fun newDetachedNode(): Node = DetachedNode()

internal actual fun newDetachedElement(): Element = DetachedElement()
