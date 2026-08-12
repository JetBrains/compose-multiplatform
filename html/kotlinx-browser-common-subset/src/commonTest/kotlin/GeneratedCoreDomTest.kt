// Exercises core DOM dictionaries, enum-like values, and array-style access in common code.
package kotlinx.browser.dom.coredom

import kotlinx.browser.JsAny
import kotlinx.browser.dom.AUTO
import kotlinx.browser.dom.CENTER
import kotlinx.browser.dom.DOMRect
import kotlinx.browser.dom.DOMRectList
import kotlinx.browser.dom.END
import kotlinx.browser.dom.NEAREST
import kotlinx.browser.dom.ScrollBehavior
import kotlinx.browser.dom.ScrollIntoViewOptions
import kotlinx.browser.dom.ScrollLogicalPosition
import kotlinx.browser.dom.SMOOTH
import kotlinx.browser.dom.START
import kotlinx.browser.dom.enumlike.areIdentical
import kotlinx.browser.dom.get
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeneratedCoreDomTest {
    @Test
    fun scrollLogicalPositionsKeepTheirTargetIdentity() {
        assertIdentical(ScrollLogicalPosition.START, ScrollLogicalPosition.START)
        assertIdentical(ScrollLogicalPosition.CENTER, ScrollLogicalPosition.CENTER)
        assertIdentical(ScrollLogicalPosition.END, ScrollLogicalPosition.END)
        assertIdentical(ScrollLogicalPosition.NEAREST, ScrollLogicalPosition.NEAREST)
        assertFalse(areIdentical(ScrollLogicalPosition.START, ScrollLogicalPosition.END))
    }

    @Test
    fun scrollIntoViewFactoryUsesNullDefaultsAndKeepsMutableInheritedState() {
        val defaults = ScrollIntoViewOptions()
        val configured = ScrollIntoViewOptions(
            block = ScrollLogicalPosition.START,
            inline = ScrollLogicalPosition.END,
            behavior = ScrollBehavior.SMOOTH,
        )

        assertNull(defaults.block)
        assertNull(defaults.`inline`)
        assertNull(defaults.behavior)
        assertIdentical(configured.block, ScrollLogicalPosition.START)
        assertIdentical(configured.`inline`, ScrollLogicalPosition.END)
        assertIdentical(configured.behavior, ScrollBehavior.SMOOTH)

        configured.block = ScrollLogicalPosition.NEAREST
        configured.`inline` = ScrollLogicalPosition.START
        configured.behavior = ScrollBehavior.AUTO

        assertIdentical(configured.block, ScrollLogicalPosition.NEAREST)
        assertIdentical(configured.`inline`, ScrollLogicalPosition.START)
        assertIdentical(configured.behavior, ScrollBehavior.AUTO)
        assertNull(defaults.block)
    }

    private fun assertIdentical(actual: JsAny?, expected: JsAny) {
        assertTrue(actual != null && areIdentical(actual, expected))
    }
}

internal fun firstRect(rects: DOMRectList): DOMRect? = rects[0]
