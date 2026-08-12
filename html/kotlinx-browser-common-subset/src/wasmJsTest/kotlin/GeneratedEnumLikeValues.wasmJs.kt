// Supplies Wasm/JS identity checks for generated enum-like values.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package kotlinx.browser.dom.enumlike

import kotlinx.browser.JsAny
import kotlinx.browser.JsString
import kotlinx.browser.toKotlinString
import kotlin.js.unsafeCast

// Wasm/JS creates a fresh handle per read, so compare the wrapped JavaScript strings.
internal actual fun areIdentical(first: JsAny, second: JsAny): Boolean =
    first.unsafeCast<JsString>().toKotlinString() == second.unsafeCast<JsString>().toKotlinString()
