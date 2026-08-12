// Provides hierarchy fixtures for browser runtime tests.
import kotlinx.browser.JsAny
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.ElementContentEditable
import kotlinx.browser.dom.HTMLAnchorElement
import kotlinx.browser.dom.HTMLButtonElement
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.HTMLFormElement
import kotlinx.browser.dom.HTMLVideoElement
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.NodeFilter
import kotlinx.browser.dom.NodeList
import kotlinx.browser.dom.Text
import kotlinx.browser.dom.ValidityState
import kotlinx.browser.dom.UnionElementOrRadioNodeList
import kotlinx.browser.dom.get
import kotlinx.browser.dom.events.CompositionEvent
import kotlinx.browser.dom.events.CompositionEventInit
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventListener
import kotlinx.browser.dom.events.EventModifierInit
import kotlinx.browser.dom.events.EventTarget
import kotlinx.browser.dom.events.FocusEvent
import kotlinx.browser.dom.events.FocusEventInit
import kotlinx.browser.dom.events.InputEvent
import kotlinx.browser.dom.events.InputEventInit
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.KeyboardEventInit
import kotlinx.browser.dom.events.MouseEvent
import kotlinx.browser.dom.events.MouseEventInit
import kotlinx.browser.dom.events.UIEvent
import kotlinx.browser.dom.events.UIEventInit
import kotlinx.browser.dom.events.WheelEvent
import kotlinx.browser.dom.events.WheelEventInit

private fun acceptsJsAny(value: JsAny): JsAny = value

private fun divAsElement(value: HTMLDivElement): Element = value

private fun divAsEventTarget(value: HTMLDivElement): EventTarget = value

private fun textAsEventTarget(value: Text): EventTarget = value

private fun videoAsEventTarget(value: HTMLVideoElement): EventTarget = value

private fun divAsJsAny(value: HTMLDivElement): JsAny = acceptsJsAny(value)

private fun useNodeMembers(node: Node, child: Node): Node {
    node.nodeValue = "value"
    node.textContent = "content"
    node.normalize()
    node.isSameNode(child)
    return node.appendChild(child)
}

private fun useElementMembers(element: HTMLDivElement) {
    element.id = "root"
    element.title = "title"
    element.align = "center"
    element.setAttribute("role", "main")
    element.scrollTo(0.0, 10.0)
}

private fun useMixinMembers(div: HTMLDivElement, anchor: HTMLAnchorElement): ElementContentEditable {
    div.contentEditable = "true"
    div.isContentEditable
    anchor.href = "https://example.org"
    anchor.hash = "#top"
    return div
}

private fun useInheritedMixinMembers(element: Element, child: Node): NodeList {
    element.append(child)
    element.prepend("leading")
    element.querySelector("#root")
    element.remove()
    return element.querySelectorAll("div")
}

private fun useTextMembers(text: Text): Text {
    text.appendData("content")
    val siblings: Element? = text.previousElementSibling ?: text.nextElementSibling
    text.before(text)
    text.before("leading")
    text.after(text)
    text.after("trailing")
    text.replaceWith(text)
    text.replaceWith("replacement")
    text.remove()
    return text.splitText(text.length)
}

private fun useButtonMembers(button: HTMLButtonElement): ValidityState {
    button.disabled = true
    button.value = "submit"
    button.setCustomValidity("")
    button.labels.length
    button.labels.item(0)
    return button.validity
}

private fun useGeneratedCompanionsAndOperators(nodes: NodeList): Int {
    val first: Node? = nodes[0]
    first?.nodeType
    return Node.ELEMENT_NODE + NodeFilter.SHOW_DOCUMENT
}

private fun useOperatorDiscoveredReturnType(form: HTMLFormElement): UnionElementOrRadioNodeList? =
    form["named-control"]

private fun useEventMembers(
    target: EventTarget,
    listener: EventListener,
    event: Event,
    composition: CompositionEvent,
    focus: FocusEvent,
    input: InputEvent,
    keyboard: KeyboardEvent,
    mouse: MouseEvent,
    wheel: WheelEvent,
): Event {
    target.addEventListener("portable", listener)
    target.addEventListener("portable", listener, false)
    target.removeEventListener("portable", listener)
    target.removeEventListener("portable", listener, false)
    target.dispatchEvent(event)
    listener.handleEvent(event)

    event.preventDefault()
    composition.data
    focus.relatedTarget
    input.isComposing
    keyboard.getModifierState("Control")
    mouse.clientX
    wheel.deltaY
    Event.AT_TARGET
    KeyboardEvent.DOM_KEY_LOCATION_NUMPAD
    WheelEvent.DOM_DELTA_PAGE

    val ui: UIEvent = wheel
    val mouseEvent: MouseEvent = wheel
    return if (ui.detail == mouseEvent.clientX) composition else wheel
}

private fun buildEventDictionaries(target: EventTarget): Array<JsAny> = arrayOf(
    UIEventInit(detail = 1),
    CompositionEventInit(data = "composition"),
    EventModifierInit(ctrlKey = true),
    FocusEventInit(relatedTarget = target),
    InputEventInit(data = "input", isComposing = true),
    KeyboardEventInit(key = "Enter"),
    MouseEventInit(clientX = 10, clientY = 20),
    WheelEventInit(deltaY = 1.0),
)

internal fun exerciseGeneratedHierarchy(
    div: HTMLDivElement,
    child: Text,
    anchor: HTMLAnchorElement,
    button: HTMLButtonElement,
    form: HTMLFormElement,
    video: HTMLVideoElement,
    listener: EventListener,
    composition: CompositionEvent,
    focus: FocusEvent,
    input: InputEvent,
    keyboard: KeyboardEvent,
    mouse: MouseEvent,
    wheel: WheelEvent,
) {
    divAsElement(div)
    divAsEventTarget(div)
    textAsEventTarget(child)
    videoAsEventTarget(video)
    divAsJsAny(div)
    useNodeMembers(div, child)
    useElementMembers(div)
    useMixinMembers(div, anchor)
    useInheritedMixinMembers(div, child)
    useTextMembers(child)
    useButtonMembers(button)
    useGeneratedCompanionsAndOperators(div.childNodes)
    useOperatorDiscoveredReturnType(form)
    useEventMembers(div, listener, Event("portable"), composition, focus, input, keyboard, mouse, wheel)
    buildEventDictionaries(div)
}
