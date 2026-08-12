// Verifies mutable event dictionary properties and source-independent null defaults.
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.browser.dom.ElementDefinitionOptions
import kotlinx.browser.dom.EventInit
import kotlinx.browser.dom.events.KeyboardEventInit
import kotlinx.browser.dom.events.WheelEventInit

/** The generated dictionary surface and its behavior, exercised unchanged on all three targets. */
class GeneratedMutableEventDictionariesTest {
    @Test
    fun baseEventDictionaryKeepsFactoryValuesAndMutations() {
        val init = EventInit(bubbles = true, cancelable = true)

        assertEquals(true, init.bubbles)
        assertEquals(true, init.cancelable)
        assertNull(init.composed)

        init.bubbles = false
        init.composed = true

        assertEquals(false, init.bubbles)
        assertEquals(true, init.composed)
    }

    @Test
    fun wheelDictionaryStoresItsCompleteInheritedSurface() {
        val init = WheelEventInit(
            deltaX = 1.25,
            deltaY = 2.5,
            deltaZ = 3.75,
            deltaMode = 2,
            screenX = 10,
            screenY = 11,
            clientX = 12,
            clientY = 13,
            button = 1,
            buttons = 3,
            region = "content",
            ctrlKey = true,
            modifierCapsLock = true,
            detail = 4,
            bubbles = true,
            composed = true,
        )

        assertEquals(1.25, init.deltaX)
        assertEquals(2.5, init.deltaY)
        assertEquals(3.75, init.deltaZ)
        assertEquals(2, init.deltaMode)
        assertEquals(10, init.screenX)
        assertEquals(11, init.screenY)
        assertEquals(12, init.clientX)
        assertEquals(13, init.clientY)
        assertEquals(1.toShort(), init.button)
        assertEquals(3.toShort(), init.buttons)
        assertNull(init.relatedTarget)
        assertEquals("content", init.region)
        assertEquals(true, init.ctrlKey)
        assertEquals(true, init.modifierCapsLock)
        assertNull(init.view)
        assertEquals(4, init.detail)
        assertEquals(true, init.bubbles)
        assertNull(init.cancelable)
        assertEquals(true, init.composed)

        init.deltaX = 9.5
        init.screenX = 20
        init.ctrlKey = false
        init.detail = 8
        init.bubbles = false

        assertEquals(9.5, init.deltaX)
        assertEquals(20, init.screenX)
        assertEquals(false, init.ctrlKey)
        assertEquals(8, init.detail)
        assertEquals(false, init.bubbles)
    }

    @Test
    fun keyboardDefaultsAreMutableAndFactoryCallsAreIsolated() {
        val first = KeyboardEventInit(key = "Enter", ctrlKey = true)
        val second = KeyboardEventInit()

        assertEquals("Enter", first.key)
        assertEquals(true, first.ctrlKey)
        assertNull(second.key)
        assertNull(second.code)
        assertNull(second.location)
        assertNull(second.repeat)
        assertNull(second.isComposing)
        assertNull(second.ctrlKey)
        assertNull(second.bubbles)

        first.key = "Escape"
        first.ctrlKey = false
        first.bubbles = true

        assertEquals("Escape", first.key)
        assertEquals(false, first.ctrlKey)
        assertEquals(true, first.bubbles)
        assertNull(second.key)
        assertNull(second.ctrlKey)
        assertNull(second.bubbles)
    }

    @Test
    fun trailingUnderscoreFactoryParameterInitializesItsKeywordProperty() {
        val definition = ElementDefinitionOptions(extends_ = "base-element")

        assertEquals("base-element", definition.extends)

        definition.extends = "changed-base"

        assertEquals("changed-base", definition.extends)
    }
}
