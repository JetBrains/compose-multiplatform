/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Covers canvas, event, network, and worker facade types in common code.
package kotlinx.browser.dom.surfaces

import kotlinx.browser.JsArray
import kotlinx.browser.JsDouble
import kotlinx.browser.dom.BroadcastChannel
import kotlinx.browser.dom.CanvasFillRule
import kotlinx.browser.dom.CanvasImageSource
import kotlinx.browser.dom.CanvasLineCap
import kotlinx.browser.dom.CanvasPattern
import kotlinx.browser.dom.CanvasRenderingContext2D
import kotlinx.browser.dom.CanvasRenderingContext2DSettings
import kotlinx.browser.dom.CLASSIC
import kotlinx.browser.dom.CloseEvent
import kotlinx.browser.dom.CloseEventInit
import kotlinx.browser.dom.CustomEvent
import kotlinx.browser.dom.CustomEventInit
import kotlinx.browser.dom.ErrorEvent
import kotlinx.browser.dom.ErrorEventInit
import kotlinx.browser.dom.EventSource
import kotlinx.browser.dom.ImageData
import kotlinx.browser.dom.MediaQueryListEvent
import kotlinx.browser.dom.MediaQueryListEventInit
import kotlinx.browser.dom.MODULE
import kotlinx.browser.dom.NONZERO
import kotlinx.browser.dom.Path2D
import kotlinx.browser.dom.ROUND
import kotlinx.browser.dom.RelatedEvent
import kotlinx.browser.dom.RelatedEventInit
import kotlinx.browser.dom.SharedWorker
import kotlinx.browser.dom.TouchEvent
import kotlinx.browser.dom.WebSocket
import kotlinx.browser.dom.Worker
import kotlinx.browser.dom.WorkerOptions
import kotlinx.browser.dom.WorkerType
import kotlinx.browser.dom.enumlike.areIdentical
import kotlinx.browser.dom.events.Event
import kotlinx.browser.fetch.RequestCredentials
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeneratedCanvasEventsNetworkWorkerTest {
    @Test
    fun dictionariesUseNullDefaultsAndKeepMutableState() {
        val canvas = CanvasRenderingContext2DSettings()
        val worker = WorkerOptions()
        val close = CloseEventInit(wasClean = true, code = 1000, reason = "done")

        assertNull(canvas.alpha)
        canvas.alpha = false
        assertEquals(false, canvas.alpha)

        assertNull(worker.type)
        worker.type = WorkerType.MODULE
        assertIdentical(worker.type, WorkerType.MODULE)

        assertEquals(true, close.wasClean)
        assertEquals(1000.toShort(), close.code)
        assertEquals("done", close.reason)
        assertNull(close.bubbles)
    }

    @Test
    fun enumValuesKeepStableDistinctIdentity() {
        assertIdentical(CanvasLineCap.ROUND, CanvasLineCap.ROUND)
        assertIdentical(CanvasFillRule.NONZERO, CanvasFillRule.NONZERO)
        assertFalse(areIdentical(WorkerType.CLASSIC, WorkerType.MODULE))
    }

    private fun assertIdentical(actual: kotlinx.browser.JsAny?, expected: kotlinx.browser.JsAny) {
        assertTrue(actual != null && areIdentical(actual, expected))
    }
}

internal fun useCanvasSurface(
    context: CanvasRenderingContext2D,
    image: CanvasImageSource,
    path: Path2D,
): CanvasPattern? {
    context.save()
    context.drawImage(image, 0.0, 0.0)
    context.fill(path, CanvasFillRule.NONZERO)
    context.lineCap = CanvasLineCap.ROUND
    context.restore()
    return context.createPattern(image, "repeat")
}

internal fun useNumericCanvasSequence(
    context: CanvasRenderingContext2D,
    values: JsArray<JsDouble>,
): JsArray<JsDouble> {
    context.setLineDash(values)
    return context.getLineDash()
}

internal fun constructCanvasTypes() {
    Path2D()
    Path2D("M0 0 L1 1")
    ImageData(1, 1)
}

internal fun constructWorkerAndNetworkTypes(credentials: RequestCredentials) {
    val options = WorkerOptions(type = WorkerType.MODULE, credentials = credentials)
    BroadcastChannel("portable").close()
    Worker("data:application/javascript,", options).terminate()
    SharedWorker("data:application/javascript,", options = options).port.close()
    EventSource("data:text/event-stream,").close()
    WebSocket("ws://127.0.0.1:9").close()
}

internal fun constructEventSubtypes() {
    CloseEvent("close", CloseEventInit())
    CustomEvent("custom", CustomEventInit())
    ErrorEvent("error", ErrorEventInit())
    MediaQueryListEvent("change", MediaQueryListEventInit())
}

// Runs only where the browser exposes this legacy constructor.
internal fun constructRelatedEvent() {
    RelatedEvent("related", RelatedEventInit())
}

internal fun eventSubtypeHierarchy(
    close: CloseEvent,
    custom: CustomEvent,
    error: ErrorEvent,
    media: MediaQueryListEvent,
    related: RelatedEvent,
    touch: TouchEvent,
): List<Event> = listOf(close, custom, error, media, related, touch)
