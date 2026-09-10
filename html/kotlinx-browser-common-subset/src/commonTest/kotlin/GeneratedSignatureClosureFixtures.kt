/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Provides signature-closure fixtures for browser runtime tests.
import kotlinx.browser.JsAny
import kotlinx.browser.dom.AddEventListenerOptions
import kotlinx.browser.dom.Document
import kotlinx.browser.dom.EventListenerOptions
import kotlinx.browser.dom.MimeType
import kotlinx.browser.dom.MimeTypeArray
import kotlinx.browser.dom.Navigator
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.Window
import kotlinx.browser.dom.events.EventListener
import kotlinx.browser.dom.events.EventTarget

private fun useDiscoveredListenerOptions(
    target: EventTarget,
    listener: EventListener,
    add: AddEventListenerOptions,
    remove: EventListenerOptions,
) {
    target.addEventListener("common", listener, add)
    target.removeEventListener("common", listener, remove)
}

private fun reachTransitively(node: Node): MimeType? {
    val document: Document = node.ownerDocument ?: return null
    val window: Window = document.defaultView ?: return null
    val navigator: Navigator = window.navigator
    val mimeTypes: MimeTypeArray = navigator.mimeTypes
    return mimeTypes.item(0)
}

private fun buildDiscoveredDictionaries(): Array<JsAny> = arrayOf(
    EventListenerOptions(capture = true),
    AddEventListenerOptions(passive = true, once = true, capture = false),
)

private fun addOptionsAreListenerOptions(options: AddEventListenerOptions): EventListenerOptions = options

internal fun exerciseGeneratedSignatureClosure(
    target: EventTarget,
    listener: EventListener,
    node: Node,
) {
    val add = AddEventListenerOptions(passive = true)
    val remove = EventListenerOptions(capture = false)
    useDiscoveredListenerOptions(target, listener, add, remove)
    reachTransitively(node)
    buildDiscoveredDictionaries()
    addOptionsAreListenerOptions(add)
}
