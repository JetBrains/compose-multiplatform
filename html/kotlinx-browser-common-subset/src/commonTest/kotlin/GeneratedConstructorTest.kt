// Exercises generated portable constructors.
import kotlinx.browser.JsArray
import kotlinx.browser.JsDouble
import kotlinx.browser.JsString
import kotlinx.browser.length
import kotlinx.browser.toJsArray
import kotlinx.browser.toJsDouble
import kotlinx.browser.dom.Comment
import kotlinx.browser.dom.Audio
import kotlinx.browser.dom.DOMMatrix
import kotlinx.browser.dom.DOMMatrixReadOnly
import kotlinx.browser.dom.DOMPoint
import kotlinx.browser.dom.DOMPointInit
import kotlinx.browser.dom.DOMQuad
import kotlinx.browser.dom.EventInit
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.Image
import kotlinx.browser.dom.MutationObserver
import kotlinx.browser.dom.MutationObserverInit
import kotlinx.browser.dom.MutationRecord
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.Option
import kotlinx.browser.dom.Text
import kotlinx.browser.dom.clipboard.ClipboardEvent
import kotlinx.browser.dom.clipboard.ClipboardEventInit
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.KeyboardEventInit
import kotlinx.browser.dom.events.MouseEvent
import kotlinx.browser.dom.events.MouseEventInit
import kotlinx.browser.dom.events.UIEvent
import kotlin.test.Test

private fun buildEvents(): List<Event> = listOf(
    Event("portable"),
    Event("portable", EventInit(bubbles = true, cancelable = true)),
    UIEvent("ui"),
    MouseEvent("click", MouseEventInit(clientX = 10, clientY = 20)),
    KeyboardEvent("keydown", KeyboardEventInit(key = "Enter")),
    ClipboardEvent("copy", ClipboardEventInit()),
)

private fun buildCharacterData(): List<Node> = listOf(Text(), Text("hello"), Comment(), Comment("note"))

private fun buildHtmlConvenienceClasses(): List<HTMLElement> = listOf(
    Audio(),
    Audio("data:audio/wav;base64,"),
    Image(),
    Image(640, 480),
    Option(),
    Option("Portable", "portable", defaultSelected = true, selected = true),
)

private fun buildMatrices(): List<DOMMatrix> = listOf(DOMMatrix(), DOMMatrix("scale(2)"), DOMMatrix(DOMMatrix()))

private fun buildNumericMatrices(): List<DOMMatrixReadOnly> {
    val values: JsArray<JsDouble> = listOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        .map(Double::toJsDouble)
        .toJsArray()
    return listOf(DOMMatrixReadOnly(values), DOMMatrix(values))
}

private fun buildGeometry(): List<DOMPoint> = listOf(
    DOMPoint(),
    DOMPoint(x = 1.0, y = 2.0),
    DOMPoint(DOMPointInit(x = 1.0)),
    DOMQuad().p1,
)

private fun observeMutations(target: Node, filter: JsArray<JsString>): JsArray<MutationRecord> {
    val observer = MutationObserver { records: JsArray<MutationRecord>, currentObserver: MutationObserver ->
        records.length
        currentObserver.disconnect()
    }
    observer.observe(target, MutationObserverInit(childList = true, attributes = true, attributeFilter = filter))
    val records = observer.takeRecords()
    observer.disconnect()
    return records
}

private fun constructedEventsKeepTheirHierarchy(): EventTarget? {
    val wheel: MouseEvent = MouseEvent("mousemove")
    val ui: UIEvent = wheel
    val event: Event = ui
    return event.target
}

class GeneratedConstructorTest {
    @Test
    fun generatedConstructorsAreCallable() {
        buildEvents()
        val nodes = buildCharacterData()
        buildHtmlConvenienceClasses()
        buildMatrices()
        buildNumericMatrices()
        buildGeometry()
        observeMutations(nodes.first(), emptyList<JsString>().toJsArray())
        constructedEventsKeepTheirHierarchy()
    }
}
