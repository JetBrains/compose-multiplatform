/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies Wasm/JS interop bridges against browser values.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

import kotlinx.browser.JsArray
import kotlinx.browser.JsDouble
import kotlinx.browser.JsNumber
import kotlinx.browser.JsString
import kotlinx.browser.Promise
import kotlinx.browser.document
import kotlinx.browser.toJsArray
import kotlinx.browser.toJsDouble
import kotlinx.browser.toJsNumber
import kotlinx.browser.toJsString
import kotlinx.browser.dom.CanvasRenderingContext2D
import kotlinx.browser.dom.DOMTokenList
import kotlinx.browser.dom.CanvasPathDrawingStyles
import kotlinx.browser.dom.DOMMatrixReadOnly
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLCanvasElement
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import kotlin.js.unsafeCast
import kotlin.test.Test

private fun commonStringIsBrowserString(value: JsString): kotlin.js.JsString = value

private fun tokenItemIsBrowserString(value: DOMTokenList): kotlin.js.JsString? = value.item(0)

private fun commonNumberIsBrowserNumber(value: JsNumber): kotlin.js.JsNumber = value

private fun eventTimeStampIsBrowserNumber(value: Event): kotlin.js.JsNumber = value.timeStamp

private fun commonDoubleIsBrowserNumber(value: JsDouble): kotlin.js.JsNumber = value

private fun lineDashIsBrowserNumberArray(
    value: CanvasPathDrawingStyles,
): kotlin.js.JsArray<kotlin.js.JsNumber> = value.getLineDash()

private fun matrixAcceptsBrowserNumberArray(value: JsArray<JsDouble>): DOMMatrixReadOnly =
    DOMMatrixReadOnly(value)

private fun commonArrayIsBrowserArray(
    value: JsArray<EventTarget>,
): kotlin.js.JsArray<EventTarget> = value

private fun eventPathIsBrowserArray(value: Event): kotlin.js.JsArray<EventTarget> = value.composedPath()

private fun attributeNamesAreBrowserStrings(
    value: Element,
): kotlin.js.JsArray<kotlin.js.JsString> = value.getAttributeNames()

private fun commonPromiseIsBrowserPromise(
    value: Promise<EventTarget>,
): kotlin.js.Promise<EventTarget> = value

class GeneratedInteropIdentityWasmJsTest {
    @Test
    fun interopBridgesAreCallable() {
        val element: Element = document.createElement("div")
        val event = Event("common")
        val context = canvasContext(document.createElement("canvas").unsafeCast<HTMLCanvasElement>())
        val numbers = listOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            .map(Double::toJsDouble)
            .toJsArray<JsDouble>()
        val targets = listOf<EventTarget>(element).toJsArray()

        commonStringIsBrowserString("common".toJsString())
        tokenItemIsBrowserString(element.classList)
        commonNumberIsBrowserNumber(1.0.toJsNumber())
        eventTimeStampIsBrowserNumber(event)
        commonDoubleIsBrowserNumber(1.0.toJsDouble())
        lineDashIsBrowserNumberArray(context)
        matrixAcceptsBrowserNumberArray(numbers)
        commonArrayIsBrowserArray(targets)
        eventPathIsBrowserArray(event)
        attributeNamesAreBrowserStrings(element)
        commonPromiseIsBrowserPromise(resolvedPromise(element))
    }
}

private fun canvasContext(canvas: HTMLCanvasElement): CanvasRenderingContext2D =
    js("canvas.getContext('2d')")

private fun resolvedPromise(value: EventTarget): kotlin.js.Promise<EventTarget> =
    js("Promise.resolve(value)")
