@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web

import kotlinx.browser.JsAny
import kotlinx.browser.dom.Element

private external interface WasmHydrationTestState : JsAny {
    val label: String
    val count: Int
}

@JsFun("(input) => JSON.parse(input)")
private external fun parseHydrationTestState(input: String): WasmHydrationTestState

@JsFun("(name, value) => { window[name] = value; }")
private external fun setWindowInt(name: String, value: Int)

@JsFun("(name) => { delete window[name]; }")
private external fun clearWindowInt(name: String)

@JsFun("(name) => window[name]")
private external fun getWindowInt(name: String): Int

@JsFun("(element, value) => { element.nonce = value; }")
private external fun setNoncePropertyInJavaScript(element: Element, value: String)

@JsFun("element => { Object.defineProperty(element, 'nonce', { value: undefined }); }")
private external fun hideNoncePropertyInJavaScript(element: Element)

internal actual fun decodeHydrationTestState(json: String): HydrationTestState {
    val state = parseHydrationTestState(json)
    return HydrationTestState(state.label, state.count)
}

internal actual fun setWindowIntProperty(name: String, value: Int) {
    setWindowInt(name, value)
}

internal actual fun clearWindowIntProperty(name: String) {
    clearWindowInt(name)
}

internal actual fun getWindowIntProperty(name: String): Int =
    getWindowInt(name)

internal actual fun setNoncePropertyForTest(element: Element, value: String) {
    setNoncePropertyInJavaScript(element, value)
}

internal actual fun hideNoncePropertyForTest(element: Element) {
    hideNoncePropertyInJavaScript(element)
}
