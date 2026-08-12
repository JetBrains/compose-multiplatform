// Supplies browser DOM fixtures for Wasm/JS.
package kotlinx.browser.dom.defaultarguments

import kotlinx.browser.document
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.Node

internal actual fun newDetachedNode(): Node = document.createTextNode("")

internal actual fun newDetachedElement(): Element = document.createElement("div")
