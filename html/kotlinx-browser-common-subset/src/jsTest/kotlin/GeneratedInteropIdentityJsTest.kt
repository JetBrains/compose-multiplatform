/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies JS interop bridges against browser values.
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

private fun commonStringIsKotlinString(value: JsString): String = value

private fun tokenItemIsKotlinString(value: DOMTokenList): String? = value.item(0)

private fun commonNumberIsKotlinNumber(value: JsNumber): Number = value

private fun eventTimeStampIsKotlinNumber(value: Event): Number = value.timeStamp

private fun commonDoubleIsKotlinDouble(value: JsDouble): Double = value

private fun lineDashIsKotlinDoubleArray(value: CanvasPathDrawingStyles): Array<Double> =
    value.getLineDash()

private fun matrixAcceptsKotlinDoubleArray(value: JsArray<JsDouble>): DOMMatrixReadOnly =
    DOMMatrixReadOnly(value)

private fun commonArrayIsKotlinArray(value: JsArray<EventTarget>): Array<EventTarget> = value

private fun eventPathIsKotlinArray(value: Event): Array<EventTarget> = value.composedPath()

private fun attributeNamesAreKotlinStrings(value: Element): Array<String> = value.getAttributeNames()

private fun commonPromiseIsBrowserPromise(
    value: Promise<EventTarget>,
): kotlin.js.Promise<EventTarget> = value

class GeneratedInteropIdentityJsTest {
    @Test
    fun interopBridgesAreCallable() {
        val element: Element = document.createElement("div")
        val event = Event("common")
        val context = canvasContext(document.createElement("canvas").unsafeCast<HTMLCanvasElement>())
        val numbers = listOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            .map(Double::toJsDouble)
            .toJsArray<JsDouble>()
        val targets = listOf<EventTarget>(element).toJsArray()

        commonStringIsKotlinString("common".toJsString())
        tokenItemIsKotlinString(element.classList)
        commonNumberIsKotlinNumber(1.0.toJsNumber())
        eventTimeStampIsKotlinNumber(event)
        commonDoubleIsKotlinDouble(1.0.toJsDouble())
        lineDashIsKotlinDoubleArray(context)
        matrixAcceptsKotlinDoubleArray(numbers)
        commonArrayIsKotlinArray(targets)
        eventPathIsKotlinArray(event)
        attributeNamesAreKotlinStrings(element)
        commonPromiseIsBrowserPromise(resolvedPromise(element))
    }
}

private fun canvasContext(canvas: HTMLCanvasElement): CanvasRenderingContext2D =
    js("canvas.getContext('2d')")

private fun resolvedPromise(value: EventTarget): kotlin.js.Promise<EventTarget> =
    js("Promise.resolve(value)")
