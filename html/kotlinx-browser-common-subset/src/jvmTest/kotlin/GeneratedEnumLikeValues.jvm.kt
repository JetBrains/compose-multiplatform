// Supplies JVM identity helpers for generated enum-like values.
package kotlinx.browser.dom.enumlike

import kotlinx.browser.JsAny

internal actual fun areIdentical(first: JsAny, second: JsAny): Boolean = first === second
