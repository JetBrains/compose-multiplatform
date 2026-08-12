// Verifies generated web typealiases and interop.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

import kotlinx.browser.document
import kotlinx.browser.toJsArray
import kotlinx.browser.window
import kotlinx.browser.JsAny as PortableJsAny
import kotlinx.browser.JsArray as PortableJsArray
import kotlinx.browser.JsNumber as PortableJsNumber
import kotlinx.browser.JsString as PortableJsString
import kotlinx.browser.Promise as PortablePromise
import kotlinx.browser.dom.DOMTokenList as PortableDOMTokenList
import kotlinx.browser.dom.DOMRectList as PortableDOMRectList
import kotlinx.browser.dom.Element as PortableElement
import kotlinx.browser.dom.ElementContentEditable as PortableElementContentEditable
import kotlinx.browser.dom.HTMLAnchorElement as PortableHTMLAnchorElement
import kotlinx.browser.dom.HTMLButtonElement as PortableHTMLButtonElement
import kotlinx.browser.dom.HTMLDivElement as PortableHTMLDivElement
import kotlinx.browser.dom.HTMLFormElement as PortableHTMLFormElement
import kotlinx.browser.dom.HTMLHyperlinkElementUtils as PortableHTMLHyperlinkElementUtils
import kotlinx.browser.dom.HTMLMediaElement as PortableHTMLMediaElement
import kotlinx.browser.dom.Location as PortableLocation
import kotlinx.browser.dom.MutationObserver as PortableMutationObserver
import kotlinx.browser.dom.MutationObserverInit as PortableMutationObserverInit
import kotlinx.browser.dom.Node as PortableNode
import kotlinx.browser.dom.NodeList as PortableNodeList
import kotlinx.browser.dom.UnionElementOrRadioNodeList as PortableUnionElementOrRadioNodeList
import kotlinx.browser.dom.ValidityState as PortableValidityState
import kotlinx.browser.dom.Window as PortableWindow
import kotlinx.browser.dom.Audio as PortableAudio
import kotlinx.browser.dom.events.Event as PortableEvent
import kotlinx.browser.dom.events.EventTarget as PortableEventTarget
import kotlinx.browser.dom.events.MouseEvent as PortableMouseEvent
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

private fun browserDivAsPortableEventTarget(value: BrowserHTMLDivElement): PortableEventTarget = value

private fun portableDivAsBrowserEventTarget(value: PortableHTMLDivElement): BrowserEventTarget = value

private fun divAsPortableEditable(value: PortableHTMLDivElement): PortableElementContentEditable = value

private fun anchorAsBrowserHyperlink(
    value: PortableHTMLAnchorElement,
): BrowserHTMLHyperlinkElementUtils = value

private fun useMovedMembers(
    div: PortableHTMLDivElement,
    hyperlink: PortableHTMLHyperlinkElementUtils,
): Boolean {
    div.contentEditable = "true"
    hyperlink.href = "https://example.org"
    return div.isContentEditable
}

private fun usePortableMembers(
    button: PortableHTMLButtonElement,
    child: PortableHTMLDivElement,
): PortableNode {
    button.disabled = true
    button.id = "submit"
    button.setAttribute("type", "submit")
    return button.appendChild(child)
}

private fun portableValidity(button: PortableHTMLButtonElement): PortableValidityState = button.validity

private fun portableLabelsAsBrowserType(button: PortableHTMLButtonElement): BrowserNodeList = button.labels

private fun portableNodeAt(nodes: PortableNodeList): PortableNode? = nodes[0]

private fun portableNamedFormItem(
    form: PortableHTMLFormElement,
): PortableUnionElementOrRadioNodeList? = form["named-control"]

private fun generatedCompanionConstantsKeepTheirBrowserValues(): Boolean =
    PortableNode.ELEMENT_NODE == BrowserNode.ELEMENT_NODE &&
        PortableEvent.CAPTURING_PHASE == BrowserEvent.CAPTURING_PHASE &&
        BrowserWheelEvent.DOM_DELTA_PAGE > 0

private fun browserNumberToPortable(value: kotlin.js.JsNumber): PortableJsNumber = value

private fun browserArrayToPortable(
    value: kotlin.js.JsArray<BrowserEventTarget>,
): PortableJsArray<PortableEventTarget> = value

private fun portableArrayToBrowser(
    value: PortableJsArray<PortableEventTarget>,
): kotlin.js.JsArray<BrowserEventTarget> = value

private fun browserPromiseToPortable(
    value: kotlin.js.Promise<BrowserEventTarget>,
): PortablePromise<PortableEventTarget> = value

private fun portablePromiseToBrowser(
    value: PortablePromise<PortableEventTarget>,
): kotlin.js.Promise<BrowserEventTarget> = value

private fun portableRectAt(value: PortableDOMRectList): BrowserDOMRect? = value[0]

private fun portableTokenItem(value: PortableDOMTokenList): PortableJsString? = value.item(0)

private fun portableTimeStamp(value: PortableEvent): PortableJsNumber = value.timeStamp

private fun portableComposedPath(value: PortableEvent): PortableJsArray<PortableEventTarget> =
    value.composedPath()

// Chrome does not implement this legacy media method.
private fun portableStartDate(value: PortableHTMLMediaElement): PortableJsAny = value.getStartDate()

// Fullscreen requires user activation; its cross-target signature remains a compile fixture.
private fun portableFullscreen(value: PortableElement): PortablePromise<*> = value.requestFullscreen()

private fun portableAncestorOrigins(
    value: PortableLocation,
): kotlin.js.JsArray<out kotlin.js.JsString> = value.ancestorOrigins

private fun portableWindowItem(value: PortableWindow, name: String): PortableJsAny? = value[name]

private fun assignBrowserHandler(element: PortableHTMLDivElement, handler: (BrowserMouseEvent) -> Unit) {
    element.onclick = handler
}

private fun readPortableHandler(element: BrowserHTMLDivElement): ((PortableMouseEvent) -> Unit)? =
    element.onclick

private fun readErrorHandler(
    window: PortableWindow,
): ((kotlin.js.JsAny?, String, Int, Int, kotlin.js.JsAny?) -> kotlin.js.JsAny?)? = window.onerror

private fun listenWithBrowserCallback(target: PortableEventTarget, callback: (BrowserEvent) -> Unit) {
    target.addEventListener("portable", callback)
}

private fun portableMutationRecords(
    observer: PortableMutationObserver,
): kotlin.js.JsArray<BrowserMutationRecord> = observer.takeRecords()

private fun portableMutationOptions(
    filter: kotlin.js.JsArray<kotlin.js.JsString>,
): BrowserMutationObserverInit = PortableMutationObserverInit(childList = true, attributeFilter = filter)

class GeneratedWebFacadeTest {
    @Test
    fun aliasesAndMembersAreCallable() {
        val div = document.createElement("div").unsafeCast<PortableHTMLDivElement>()
        val anchor = document.createElement("a").unsafeCast<PortableHTMLAnchorElement>()
        val button = document.createElement("button").unsafeCast<PortableHTMLButtonElement>()
        val form = document.createElement("form").unsafeCast<PortableHTMLFormElement>()
        val child = document.createElement("span").unsafeCast<PortableHTMLDivElement>()
        val event = BrowserEvent("portable")
        val listener: (BrowserEvent) -> Unit = { }

        browserDivAsPortableEventTarget(div)
        portableDivAsBrowserEventTarget(div)
        divAsPortableEditable(div)
        anchorAsBrowserHyperlink(anchor)
        useMovedMembers(div, anchor)
        usePortableMembers(button, child)
        portableValidity(button)
        portableLabelsAsBrowserType(button)
        portableNodeAt(div.childNodes)
        portableNamedFormItem(form)
        assertTrue(generatedCompanionConstantsKeepTheirBrowserValues())
        browserNumberToPortable(browserNumber())

        val array = listOf<PortableEventTarget>(div).toJsArray()
        browserArrayToPortable(array)
        portableArrayToBrowser(array)
        val promise = resolvedPromise(div)
        browserPromiseToPortable(promise)
        portablePromiseToBrowser(promise)

        val rects = rectList()
        portableRectAt(rects)
        firstRect(rects)
        portableTokenItem(div.classList)
        portableTimeStamp(event)
        portableComposedPath(event)
        val audio = PortableAudio()
        if (hasStartDate(audio)) portableStartDate(audio)
        document.body?.appendChild(div)
        ignoreRejection(portableFullscreen(div))
        portableAncestorOrigins(window.location)
        portableWindowItem(window, "0")
        assignBrowserHandler(div) { }
        readPortableHandler(div)
        readErrorHandler(window)
        listenWithBrowserCallback(div, listener)

        val observer = PortableMutationObserver { _, _ -> }
        portableMutationRecords(observer)
        portableMutationOptions(emptyList<PortableJsString>().toJsArray())
        observer.disconnect()
        div.remove()
    }
}

private fun resolvedPromise(value: BrowserEventTarget): kotlin.js.Promise<BrowserEventTarget> =
    js("Promise.resolve(value)")

private fun browserNumber(): kotlin.js.JsNumber = js("1")

private fun rectList(): PortableDOMRectList =
    js("({ 0: new DOMRect(), length: 1, item: function(index) { return this[index] || null; } })")

private fun hasStartDate(value: PortableHTMLMediaElement): Boolean =
    js("typeof value.getStartDate === 'function'")

private fun ignoreRejection(promise: PortablePromise<*>): Unit =
    js("void promise.catch(function() {})")
