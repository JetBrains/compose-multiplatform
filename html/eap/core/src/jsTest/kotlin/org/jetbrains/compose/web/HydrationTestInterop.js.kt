package org.jetbrains.compose.web

import kotlinx.browser.dom.Element
import kotlinx.browser.window
import kotlin.js.JSON

private external interface JsHydrationTestState {
    val label: String
    val count: Int
}

internal actual fun decodeHydrationTestState(json: String): HydrationTestState {
    val state = JSON.parse<JsHydrationTestState>(json)
    return HydrationTestState(state.label, state.count)
}

internal actual fun setWindowIntProperty(name: String, value: Int) {
    window.asDynamic()[name] = value
}

internal actual fun clearWindowIntProperty(name: String) {
    window.asDynamic()[name] = js("undefined")
}

internal actual fun getWindowIntProperty(name: String): Int =
    window.asDynamic()[name]

internal actual fun setNoncePropertyForTest(element: Element, value: String) {
    element.asDynamic().nonce = value
}

internal actual fun hideNoncePropertyForTest(element: Element) {
    js("Object").defineProperty(element, "nonce", js("({value: undefined})"))
}
