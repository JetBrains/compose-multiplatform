/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies JVM hierarchy and state for canvas, event, network, and worker facades.
package kotlinx.browser.dom.surfaces

import kotlinx.browser.dom.CanvasPath
import kotlinx.browser.dom.CloseEvent
import kotlinx.browser.dom.CloseEventInit
import kotlinx.browser.dom.CustomEvent
import kotlinx.browser.dom.ErrorEvent
import kotlinx.browser.dom.ImageBitmapSource
import kotlinx.browser.dom.ImageData
import kotlinx.browser.dom.MediaQueryListEvent
import kotlinx.browser.dom.Path2D
import kotlinx.browser.dom.RelatedEvent
import kotlinx.browser.dom.SharedWorker
import kotlinx.browser.dom.Worker
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GeneratedCanvasEventsNetworkWorkerJvmTest {
    @Test
    fun abstractWorkerOnErrorIsStoredOnBothConcreteWorkers() {
        val worker = Worker("worker.js")
        val sharedWorker = SharedWorker("shared-worker.js")
        var calls = 0
        val handler: (Event) -> Unit = { calls++ }

        worker.onerror = handler
        sharedWorker.onerror = handler
        worker.onerror?.invoke(Event("worker-error"))
        sharedWorker.onerror?.invoke(Event("shared-worker-error"))

        assertEquals(2, calls)
        assertIs<EventTarget>(worker)
        assertIs<EventTarget>(sharedWorker)
    }

    @Test
    fun canvasAndEventStubsKeepTheirPortableHierarchy() {
        assertIs<CanvasPath>(Path2D())
        assertIs<ImageBitmapSource>(ImageData(2, 3))
        assertIs<Event>(CloseEvent("close", CloseEventInit()))
        assertIs<Event>(CustomEvent("custom"))
        assertIs<Event>(ErrorEvent("error"))
        assertIs<Event>(MediaQueryListEvent("change"))
        assertIs<Event>(RelatedEvent("related"))
    }
}
