/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Provides portable callback fixtures for browser runtime tests.
import kotlinx.browser.JsAny
import kotlinx.browser.JsArray
import kotlinx.browser.JsString
import kotlinx.browser.dom.Document
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.MutationObserver
import kotlinx.browser.dom.MutationObserverInit
import kotlinx.browser.dom.MutationRecord
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.TreeWalker
import kotlinx.browser.dom.Window
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventListener
import kotlinx.browser.dom.events.EventTarget
import kotlinx.browser.dom.events.MouseEvent

// Handler properties cover the most common nullable callback shape.
private fun assignEventHandlers(element: HTMLElement, window: Window) {
    element.onclick = { event: MouseEvent -> element.id = event.type }
    element.onblur = null
    window.onhashchange = { }
}

private fun filterNodes(document: Document, root: Node): TreeWalker =
    document.createTreeWalker(root, filter = { node: Node -> node.nodeType })

private fun assignErrorHandler(window: Window) {
    window.onerror = { event: JsAny?, _: String, _: Int, _: Int, _: JsAny? -> event }
}

private fun scheduleWork(window: Window): Int = window.requestAnimationFrame { timestamp: Double ->
    window.name = timestamp.toString()
}

// Exercise both the interface and callback listener overloads.
private fun listenBothWays(target: EventTarget, listener: EventListener) {
    target.addEventListener("portable", listener)
    target.addEventListener("portable") { event: Event -> event.preventDefault() }
    target.removeEventListener("portable", listener, options = false)
    target.removeEventListener("portable", { event: Event -> event.stopPropagation() }, options = false)
}

private fun observeMutations(observer: MutationObserver, target: Node, filter: JsArray<JsString>) {
    observer.observe(target, MutationObserverInit(childList = true, attributes = true, attributeFilter = filter))
    observer.disconnect()
}

private fun latestMutation(observer: MutationObserver): JsArray<MutationRecord> = observer.takeRecords()

internal fun exerciseGeneratedCallbacks(
    element: HTMLElement,
    window: Window,
    document: Document,
    root: Node,
    listener: EventListener,
    observer: MutationObserver,
    filter: JsArray<JsString>,
) {
    assignEventHandlers(element, window)
    filterNodes(document, root)
    assignErrorHandler(window)
    window.cancelAnimationFrame(scheduleWork(window))
    listenBothWays(element, listener)
    observeMutations(observer, root, filter)
    latestMutation(observer)
    observer.disconnect()
}
