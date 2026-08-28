/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies generated web typealiases and interop.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

import kotlinx.browser.document
import kotlinx.browser.toJsArray
import kotlinx.browser.window
import kotlinx.browser.JsAny as CommonJsAny
import kotlinx.browser.JsArray as CommonJsArray
import kotlinx.browser.JsNumber as CommonJsNumber
import kotlinx.browser.JsString as CommonJsString
import kotlinx.browser.Promise as CommonPromise
import kotlinx.browser.dom.DOMTokenList as CommonDOMTokenList
import kotlinx.browser.dom.DOMRectList as CommonDOMRectList
import kotlinx.browser.dom.Element as CommonElement
import kotlinx.browser.dom.ElementContentEditable as CommonElementContentEditable
import kotlinx.browser.dom.HTMLAnchorElement as CommonHTMLAnchorElement
import kotlinx.browser.dom.HTMLButtonElement as CommonHTMLButtonElement
import kotlinx.browser.dom.HTMLDivElement as CommonHTMLDivElement
import kotlinx.browser.dom.HTMLFormElement as CommonHTMLFormElement
import kotlinx.browser.dom.HTMLHyperlinkElementUtils as CommonHTMLHyperlinkElementUtils
import kotlinx.browser.dom.HTMLMediaElement as CommonHTMLMediaElement
import kotlinx.browser.dom.Location as CommonLocation
import kotlinx.browser.dom.MutationObserver as CommonMutationObserver
import kotlinx.browser.dom.MutationObserverInit as CommonMutationObserverInit
import kotlinx.browser.dom.Node as CommonNode
import kotlinx.browser.dom.NodeList as CommonNodeList
import kotlinx.browser.dom.UnionElementOrRadioNodeList as CommonUnionElementOrRadioNodeList
import kotlinx.browser.dom.ValidityState as CommonValidityState
import kotlinx.browser.dom.Window as CommonWindow
import kotlinx.browser.dom.Audio as CommonAudio
import kotlinx.browser.dom.events.Event as CommonEvent
import kotlinx.browser.dom.events.EventTarget as CommonEventTarget
import kotlinx.browser.dom.events.MouseEvent as CommonMouseEvent
import kotlinx.browser.dom.coredom.firstRect
import kotlinx.browser.dom.get
import org.w3c.dom.DOMRect as BrowserDOMRect
import org.w3c.dom.HTMLDivElement as BrowserHTMLDivElement
import org.w3c.dom.HTMLHyperlinkElementUtils as BrowserHTMLHyperlinkElementUtils
import org.w3c.dom.MutationObserverInit as BrowserMutationObserverInit
import org.w3c.dom.MutationRecord as BrowserMutationRecord
import org.w3c.dom.Node as BrowserNode
import org.w3c.dom.NodeList as BrowserNodeList
import org.w3c.dom.events.Event as BrowserEvent
import org.w3c.dom.events.EventTarget as BrowserEventTarget
import org.w3c.dom.events.MouseEvent as BrowserMouseEvent
import org.w3c.dom.events.WheelEvent as BrowserWheelEvent
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertTrue

private fun browserDivAsCommonEventTarget(value: BrowserHTMLDivElement): CommonEventTarget = value

private fun commonDivAsBrowserEventTarget(value: CommonHTMLDivElement): BrowserEventTarget = value

private fun divAsCommonEditable(value: CommonHTMLDivElement): CommonElementContentEditable = value

private fun anchorAsBrowserHyperlink(
    value: CommonHTMLAnchorElement,
): BrowserHTMLHyperlinkElementUtils = value

private fun useMovedMembers(
    div: CommonHTMLDivElement,
    hyperlink: CommonHTMLHyperlinkElementUtils,
): Boolean {
    div.contentEditable = "true"
    hyperlink.href = "https://example.org"
    return div.isContentEditable
}

private fun useCommonMembers(
    button: CommonHTMLButtonElement,
    child: CommonHTMLDivElement,
): CommonNode {
    button.disabled = true
    button.id = "submit"
    button.setAttribute("type", "submit")
    return button.appendChild(child)
}

private fun commonValidity(button: CommonHTMLButtonElement): CommonValidityState = button.validity

private fun commonLabelsAsBrowserType(button: CommonHTMLButtonElement): BrowserNodeList = button.labels

private fun commonNodeAt(nodes: CommonNodeList): CommonNode? = nodes[0]

private fun commonNamedFormItem(
    form: CommonHTMLFormElement,
): CommonUnionElementOrRadioNodeList? = form["named-control"]

private fun generatedCompanionConstantsKeepTheirBrowserValues(): Boolean =
    CommonNode.ELEMENT_NODE == BrowserNode.ELEMENT_NODE &&
        CommonEvent.CAPTURING_PHASE == BrowserEvent.CAPTURING_PHASE &&
        BrowserWheelEvent.DOM_DELTA_PAGE > 0

private fun browserNumberToCommon(value: kotlin.js.JsNumber): CommonJsNumber = value

private fun browserArrayToCommon(
    value: kotlin.js.JsArray<BrowserEventTarget>,
): CommonJsArray<CommonEventTarget> = value

private fun commonArrayToBrowser(
    value: CommonJsArray<CommonEventTarget>,
): kotlin.js.JsArray<BrowserEventTarget> = value

private fun browserPromiseToCommon(
    value: kotlin.js.Promise<BrowserEventTarget>,
): CommonPromise<CommonEventTarget> = value

private fun commonPromiseToBrowser(
    value: CommonPromise<CommonEventTarget>,
): kotlin.js.Promise<BrowserEventTarget> = value

private fun commonRectAt(value: CommonDOMRectList): BrowserDOMRect? = value[0]

private fun commonTokenItem(value: CommonDOMTokenList): CommonJsString? = value.item(0)

private fun commonTimeStamp(value: CommonEvent): CommonJsNumber = value.timeStamp

private fun commonComposedPath(value: CommonEvent): CommonJsArray<CommonEventTarget> =
    value.composedPath()

// Chrome does not implement this legacy media method.
private fun commonStartDate(value: CommonHTMLMediaElement): CommonJsAny = value.getStartDate()

// Fullscreen requires user activation; its cross-target signature remains a compile fixture.
private fun commonFullscreen(value: CommonElement): CommonPromise<*> = value.requestFullscreen()

private fun commonAncestorOrigins(
    value: CommonLocation,
): kotlin.js.JsArray<out kotlin.js.JsString> = value.ancestorOrigins

private fun commonWindowItem(value: CommonWindow, name: String): CommonJsAny? = value[name]

private fun assignBrowserHandler(element: CommonHTMLDivElement, handler: (BrowserMouseEvent) -> Unit) {
    element.onclick = handler
}

private fun readCommonHandler(element: BrowserHTMLDivElement): ((CommonMouseEvent) -> Unit)? =
    element.onclick

private fun readErrorHandler(
    window: CommonWindow,
): ((kotlin.js.JsAny?, String, Int, Int, kotlin.js.JsAny?) -> kotlin.js.JsAny?)? = window.onerror

private fun listenWithBrowserCallback(target: CommonEventTarget, callback: (BrowserEvent) -> Unit) {
    target.addEventListener("common", callback)
}

private fun commonMutationRecords(
    observer: CommonMutationObserver,
): kotlin.js.JsArray<BrowserMutationRecord> = observer.takeRecords()

private fun commonMutationOptions(
    filter: kotlin.js.JsArray<kotlin.js.JsString>,
): BrowserMutationObserverInit = CommonMutationObserverInit(childList = true, attributeFilter = filter)

class GeneratedWebFacadeTest {
    @Test
    fun aliasesAndMembersAreCallable() {
        val div = document.createElement("div").unsafeCast<CommonHTMLDivElement>()
        val anchor = document.createElement("a").unsafeCast<CommonHTMLAnchorElement>()
        val button = document.createElement("button").unsafeCast<CommonHTMLButtonElement>()
        val form = document.createElement("form").unsafeCast<CommonHTMLFormElement>()
        val child = document.createElement("span").unsafeCast<CommonHTMLDivElement>()
        val event = BrowserEvent("common")
        val listener: (BrowserEvent) -> Unit = { }

        browserDivAsCommonEventTarget(div)
        commonDivAsBrowserEventTarget(div)
        divAsCommonEditable(div)
        anchorAsBrowserHyperlink(anchor)
        useMovedMembers(div, anchor)
        useCommonMembers(button, child)
        commonValidity(button)
        commonLabelsAsBrowserType(button)
        commonNodeAt(div.childNodes)
        commonNamedFormItem(form)
        assertTrue(generatedCompanionConstantsKeepTheirBrowserValues())
        browserNumberToCommon(browserNumber())

        val array = listOf<CommonEventTarget>(div).toJsArray()
        browserArrayToCommon(array)
        commonArrayToBrowser(array)
        val promise = resolvedPromise(div)
        browserPromiseToCommon(promise)
        commonPromiseToBrowser(promise)

        val rects = rectList()
        commonRectAt(rects)
        firstRect(rects)
        commonTokenItem(div.classList)
        commonTimeStamp(event)
        commonComposedPath(event)
        val audio = CommonAudio()
        if (hasStartDate(audio)) commonStartDate(audio)
        document.body?.appendChild(div)
        ignoreRejection(commonFullscreen(div))
        commonAncestorOrigins(window.location)
        commonWindowItem(window, "0")
        assignBrowserHandler(div) { }
        readCommonHandler(div)
        readErrorHandler(window)
        listenWithBrowserCallback(div, listener)

        val observer = CommonMutationObserver { _, _ -> }
        commonMutationRecords(observer)
        commonMutationOptions(emptyList<CommonJsString>().toJsArray())
        observer.disconnect()
        div.remove()
    }
}

private fun resolvedPromise(value: BrowserEventTarget): kotlin.js.Promise<BrowserEventTarget> =
    js("Promise.resolve(value)")

private fun browserNumber(): kotlin.js.JsNumber = js("1")

private fun rectList(): CommonDOMRectList =
    js("({ 0: new DOMRect(), length: 1, item: function(index) { return this[index] || null; } })")

private fun hasStartDate(value: CommonHTMLMediaElement): Boolean =
    js("typeof value.getStartDate === 'function'")

private fun ignoreRejection(promise: CommonPromise<*>): Unit =
    js("void promise.catch(function() {})")
