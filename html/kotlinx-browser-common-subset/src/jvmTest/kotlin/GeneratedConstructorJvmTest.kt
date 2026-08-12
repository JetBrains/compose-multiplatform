// Verifies runtime behavior of generated JVM constructors.
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.browser.JsAny
import kotlinx.browser.JsArray
import kotlinx.browser.JsDouble
import kotlinx.browser.length
import kotlinx.browser.toJsArray
import kotlinx.browser.toJsDouble
import kotlinx.browser.dom.Comment
import kotlinx.browser.dom.DOMMatrix
import kotlinx.browser.dom.DOMMatrixReadOnly
import kotlinx.browser.dom.DOMPoint
import kotlinx.browser.dom.DOMPointInit
import kotlinx.browser.dom.EventInit
import kotlinx.browser.dom.MutationObserver
import kotlinx.browser.dom.MutationRecord
import kotlinx.browser.dom.Text
import kotlinx.browser.dom.events.CompositionEvent
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.MouseEvent
import kotlinx.browser.dom.events.MouseEventInit
import kotlinx.browser.dom.events.UIEvent

// Verifies JVM constructor stubs are callable and store matching mutable properties.
class GeneratedConstructorJvmTest {
    @Test
    fun browserConstructorsAreCallableWithTheirDeclaredArguments() {
        assertIs<JsAny>(Event("portable"))
        assertIs<Event>(Event("portable", EventInit(bubbles = true)))
        assertIs<Event>(UIEvent("ui"))
        assertIs<UIEvent>(MouseEvent("click"))
        assertIs<MouseEvent>(MouseEvent("click", MouseEventInit(clientX = 10, clientY = 20)))

        val values: JsArray<JsDouble> = listOf(1.0.toJsDouble(), 0.0.toJsDouble()).toJsArray()
        assertIs<DOMMatrixReadOnly>(DOMMatrixReadOnly(values))
        assertIs<DOMMatrix>(DOMMatrix(values))
    }

    // Ensures defaults resolve to JVM actual values rather than `definedExternally`.
    @Test
    fun defaultedConstructorArgumentsResolveToTheJvmValue() {
        assertIs<Event>(Event("portable"))
        assertIs<Text>(Text())
        assertIs<Comment>(Comment())
        assertIs<DOMMatrix>(DOMMatrix())
        // Every parameter of this one defaults, which is what makes `DOMPoint()` legal even though
        // the browser gives the class no primary constructor.
        assertIs<DOMPoint>(DOMPoint())
    }

    // JVM stubs store constructor arguments only in matching mutable properties.
    @Test
    fun aConstructorStoresOnlyWhatTheClassHasAMutablePropertyFor() {
        assertEquals("hello", Text("hello").data)
        assertEquals("", Text().data)
        assertEquals("note", Comment("note").data)
        assertEquals("", Event("portable").type)
    }

    // Pins the protected no-argument constructor needed by JVM subclasses.
    @Test
    fun aConstructorBearingClassCanStillBeSubclassedWithoutArguments() {
        val inherited = InheritedEvent()
        val chosen = ChosenEvent()

        assertIs<Event>(inherited)
        assertIs<CompositionEvent>(chosen)
        assertEquals("", inherited.type)
        // The subclass that picks its own arguments reaches the same constructor portable code does.
        assertEquals("", chosen.type)
    }

    /** A constructor builds a new instance every time, rather than handing back a shared stub. */
    @Test
    fun constructedStubsAreDistinctInstances() {
        assertNotSame(Event("portable"), Event("portable"))
        assertNotSame(Text("hello"), Text("hello"))

        val text = Text("hello")
        assertSame(text, text.splitText(0))
    }

    // Verifies recursive type mapping accepts a generic callback constructor.
    @Test
    fun theMutationObserverCallbackConstructorAcceptsARealFunction() {
        var invoked = false
        val observer = MutationObserver { records: JsArray<MutationRecord>, _: MutationObserver ->
            invoked = true
            records.length
        }

        assertEquals(0, observer.takeRecords().length)
        assertTrue(!invoked, "nothing dispatches a mutation on the JVM")
    }

    /** A secondary constructor delegates to the primary, so it builds the same kind of stub. */
    @Test
    fun secondaryConstructorsBuildTheSameStub() {
        assertIs<DOMMatrix>(DOMMatrix("scale(2)"))
        assertIs<DOMMatrix>(DOMMatrix(DOMMatrix()))
        assertIs<DOMPoint>(DOMPoint(DOMPointInit(x = 1.0)))
        assertIs<KeyboardEvent>(KeyboardEvent("keydown"))
    }
}

/** Reaches the protected no-argument constructor the generator adds for exactly this. */
private class InheritedEvent : Event()

/** Reaches the browser constructor instead, which a subclass may just as well do. */
private class ChosenEvent : CompositionEvent("compositionstart")
