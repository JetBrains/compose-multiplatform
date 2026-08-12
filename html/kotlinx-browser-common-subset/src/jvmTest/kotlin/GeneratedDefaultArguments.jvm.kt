// Supplies JVM stubs for generated default arguments.
package kotlinx.browser.dom.defaultarguments

import kotlinx.browser.dom.Element
import kotlinx.browser.dom.Node

private class DetachedNode : Node()

private class DetachedElement : Element()

internal actual fun newDetachedNode(): Node = DetachedNode()

internal actual fun newDetachedElement(): Element = DetachedElement()
