// Verifies representative JVM facade members and hierarchy.
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.browser.JsAny
import kotlinx.browser.JsArray
import kotlinx.browser.Promise
import kotlinx.browser.length
import kotlinx.browser.toDouble
import kotlinx.browser.toJsArray
import kotlinx.browser.toJsString
import kotlinx.browser.toList
import kotlinx.browser.dom.*
import kotlinx.browser.dom.events.*

class GeneratedFacadeJvmTest {
    @Test
    fun generatedHierarchyLoadsInTheJvmSafePackage() {
        val div: Element = TestDivElement()

        assertIs<EventTarget>(div)
        assertIs<JsAny>(div)
    }

    // Loads every ledger-selected classifier from its JVM facade name.
    @Test
    fun allFacadeClassifiersLoadFromTheSafePackage() {
        val report = GeneratedModelReport.read()

        assertEquals(report.counts.getValue("closure"), report.declarations.size)
        report.declarations.forEach { declaration ->
            assertEquals(declaration.portableName, Class.forName(declaration.portableName).name)
        }
    }

    // Verifies inert JVM members store properties and return safe stub values.
    @Test
    fun generatedMemberStubsAreUsableOnJvm() {
        val div = TestDivElement()
        div.id = "root"
        div.title = "Portable title"
        div.align = "center"
        div.innerHTML = "content"

        assertEquals("root", div.id)
        assertEquals("Portable title", div.title)
        assertEquals("center", div.align)
        assertEquals("content", div.innerHTML)
        assertFalse(div.hasAttributes())
        assertSame(div, div.appendChild(div))
        assertSame(div, div.removeChild(div))
        // Defaults come from the JVM actual, not from the `definedExternally` placeholder.
        assertSame(div, div.cloneNode())
        assertSame(div, div.getRootNode())
        // String inputs are usually keys or payloads, so they must not masquerade as results.
        assertNull(div.getAttribute("id"))
        assertEquals("", TestWindow().btoa("portable"))
        assertNull(div.querySelector("#root"))
        assertEquals(0, div.querySelectorAll("div").length)

        val text = Text()
        text.data = "hello world"

        assertEquals("hello world", text.data)
        assertSame(text, text.splitText(5))

        val button = TestButtonElement()
        button.disabled = true
        button.value = "submit"
        button.click()

        assertEquals(true, button.disabled)
        assertEquals("submit", button.value)
        assertFalse(button.checkValidity())
        assertFalse(button.validity.valid)
        assertEquals(0, button.labels.length)
        assertNull(button.labels.item(0))
    }

    // Verifies interop members return their target-specific inert values on JVM.
    @Test
    fun generatedInteropStubsAreUsableOnJvm() {
        val div = TestDivElement()
        val event = Event("portable")

        assertEquals(0.0, event.timeStamp.toDouble())
        assertEquals(0, event.composedPath().length)
        assertEquals(emptyList(), event.composedPath().toList())
        assertEquals(0, div.getAttributeNames().length)
        // Nullable results stay null rather than becoming an empty value.
        assertNull(TestTokenList().item(0))
        // Opaque on both counts: a promise the JVM never resolves, and a bare JsAny.
        assertIs<Promise<*>>(div.requestFullscreen())
        assertIs<JsAny>(TestVideoElement().getStartDate())
    }

    // Verifies JVM callback properties store lambdas and callback overloads accept them.
    @Test
    fun generatedCallbackStubsAreUsableOnJvm() {
        val div = TestDivElement()
        val target = TestEventTarget()
        var clicked: MouseEvent? = null

        assertNull(div.onclick)
        div.onclick = { event -> clicked = event }
        div.onclick?.invoke(MouseEvent("click"))
        assertNotNull(clicked)

        // The five-parameter handler, the one whose interop types are nested inside a callback.
        div.onerror = { event, _, _, _, _ -> event }
        assertSame(EMPTY, div.onerror?.invoke(EMPTY, "portable", 0, 0, null))

        // Both overloads of every listener method exist now; a lambda resolves to the callback one.
        target.addEventListener("portable") { event -> event.preventDefault() }
        target.removeEventListener("portable", { }, options = false)
        assertEquals(0, TestWindow().requestAnimationFrame { })
    }

    // Exercises nested generic signatures discovered around `MutationObserver`.
    @Test
    fun mutationObserverStubsCarryTheirNestedGenerics() {
        val observer = MutationObserver { records, currentObserver ->
            records.length
            currentObserver.disconnect()
        }
        val records: JsArray<MutationRecord> = observer.takeRecords()

        assertEquals(0, records.length)
        assertEquals(emptyList(), records.toList())

        observer.observe(TestDivElement())
        observer.observe(
            TestDivElement(),
            MutationObserverInit(attributeFilter = listOf("class".toJsString()).toJsArray()),
        )
        observer.disconnect()
    }

    @Test
    fun generatedCompanionConstantsAndOperatorsAreUsableOnJvm() {
        // Repeated inherited names share values; distinct names of the same type stay distinct.
        assertEquals(Node.ELEMENT_NODE, HTMLDivElement.ELEMENT_NODE)
        assertEquals(
            Node.DOCUMENT_POSITION_CONTAINED_BY,
            HTMLDivElement.DOCUMENT_POSITION_CONTAINED_BY,
        )
        assertEquals(Event.AT_TARGET, WheelEvent.AT_TARGET)
        assertNotEquals(Node.ELEMENT_NODE, Node.ATTRIBUTE_NODE)
        assertNotEquals(NodeFilter.SHOW_ALL, NodeFilter.SHOW_ELEMENT)
        assertNotEquals(Event.AT_TARGET, Event.BUBBLING_PHASE)
        assertNotEquals(KeyboardEvent.DOM_KEY_LOCATION_NUMPAD, KeyboardEvent.DOM_KEY_LOCATION_LEFT)
        assertNotEquals(WheelEvent.DOM_DELTA_PAGE, WheelEvent.DOM_DELTA_LINE)

        // The generated JVM operator finds and delegates to NodeList.item(Int).
        val labels = TestButtonElement().labels
        assertEquals(labels.item(0), labels[0])
        assertNull(labels[0])

        // This operator's return classifier was discovered from the extension signature itself.
        assertNull(TestFormElement()["named-control"])
    }

    @Test
    fun optionDictionariesAreBuiltFromTheGeneratedFactories() {
        assertIs<GetRootNodeOptions>(GetRootNodeOptions())
        assertIs<ScrollToOptions>(ScrollToOptions(left = 0.0, top = 10.0))

        // The browser factory renames the keyword property to `param_is`; the generator maps that
        // spelling back when its JVM implementation stores the property.
        val creation = ElementCreationOptions(param_is = "portable-element")
        assertEquals("portable-element", creation.`is`)
        creation.`is` = "changed-element"
        assertEquals("changed-element", creation.`is`)

        // Selected from the exact input file alongside the EventTarget overloads that consume them.
        val listener = EventListenerOptions(capture = true)
        val add = AddEventListenerOptions(passive = true, once = true, capture = false)
        assertIs<EventListenerOptions>(listener)
        assertEquals(true, listener.capture)
        assertIs<AddEventListenerOptions>(add)
        assertIs<EventListenerOptions>(add)
        assertEquals(true, add.passive)
        assertEquals(true, add.once)
        assertEquals(false, add.capture)

        add.capture = true
        assertEquals(true, add.capture)
    }

    @Test
    fun eventHierarchyAndBehavioralInterfaceAreUsableOnJvm() {
        val event = Event("portable")
        val wheel = WheelEvent("wheel")
        val target = TestEventTarget()
        var handled: Event? = null
        val listener = object : EventListener {
            override fun handleEvent(event: Event) {
                handled = event
            }
        }

        assertIs<JsAny>(event)
        assertIs<Event>(CompositionEvent("compositionstart"))
        assertIs<Event>(FocusEvent("focus"))
        assertIs<Event>(InputEvent("input"))
        assertIs<Event>(KeyboardEvent("keydown"))
        assertIs<Event>(wheel)
        assertIs<UIEvent>(wheel)
        assertIs<MouseEvent>(wheel)

        assertEquals("", event.type)
        assertNull(event.target)
        assertFalse(event.defaultPrevented)
        event.preventDefault()
        event.initEvent("portable", bubbles = true, cancelable = true)

        listener.handleEvent(event)
        assertSame(event, handled)
        target.addEventListener("portable", listener)
        target.addEventListener("portable", listener, false)
        target.removeEventListener("portable", listener)
        target.removeEventListener("portable", listener, false)
        // The overloads discovery recovered, next to the boolean ones that were always portable.
        target.addEventListener("portable", listener, AddEventListenerOptions(once = true))
        target.removeEventListener("portable", listener, EventListenerOptions(capture = true))
        assertFalse(target.dispatchEvent(event))
    }

    @Test
    fun everyEventDictionaryFactoryReturnsItsMappedInterface() {
        val target = TestEventTarget()

        assertIs<UIEventInit>(UIEventInit(detail = 1))
        assertIs<CompositionEventInit>(CompositionEventInit(data = "composition"))
        assertIs<EventModifierInit>(EventModifierInit(ctrlKey = true))
        assertIs<FocusEventInit>(FocusEventInit(relatedTarget = target))
        assertIs<InputEventInit>(InputEventInit(data = "input", isComposing = true))
        assertIs<KeyboardEventInit>(KeyboardEventInit(key = "Enter"))
        assertIs<MouseEventInit>(MouseEventInit(clientX = 10, clientY = 20))
        assertIs<WheelEventInit>(WheelEventInit(deltaY = 1.0))

        assertIs<UIEventInit>(FocusEventInit())
        assertIs<EventModifierInit>(KeyboardEventInit())
        assertIs<MouseEventInit>(WheelEventInit())
    }

    // Ensures implementing classes store mutable members inherited from behavioral interfaces.
    @Test
    fun behavioralInterfaceMembersRoundTripThroughTheImplementingClass() {
        val div = TestDivElement()

        assertIs<ElementContentEditable>(div)
        assertFalse(div.isContentEditable)

        div.contentEditable = "true"
        assertEquals("true", div.contentEditable)

        // Through the interface type rather than the class, so the stored override really implements it.
        val editable: ElementContentEditable = div
        editable.contentEditable = "plaintext-only"
        assertEquals("plaintext-only", div.contentEditable)

        // Each implementor stores the mixin separately; HTMLElement is not where `href` lives.
        val anchor: HTMLHyperlinkElementUtils = TestAnchorElement()
        anchor.href = "https://example.org"

        assertEquals("https://example.org", anchor.href)
        assertEquals("", TestAreaElement().href)
    }
}

private class TestDivElement : HTMLDivElement()

private class TestButtonElement : HTMLButtonElement()

private class TestAnchorElement : HTMLAnchorElement()

private class TestAreaElement : HTMLAreaElement()

private class TestFormElement : HTMLFormElement()

private class TestEventTarget : EventTarget()

private class TestVideoElement : HTMLVideoElement()

private class TestTokenList : DOMTokenList()

private class TestWindow : Window()

/** A stand-in for the opaque JS value the five-parameter error handler is declared over. */
private val EMPTY = object : JsAny {}
