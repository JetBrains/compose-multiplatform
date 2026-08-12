// Verifies JVM behavior for the core DOM array-style facade.
package kotlinx.browser.dom.coredom

import kotlinx.browser.dom.DOMRect
import kotlinx.browser.dom.DOMRectList
import kotlinx.browser.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class GeneratedCoreDomJvmTest {
    @Test
    fun domRectListOperatorDelegatesToItem() {
        val rect = DOMRect(1.0, 2.0, 3.0, 4.0)
        val rects = TestDOMRectList(rect)

        assertEquals(1, rects.length)
        assertSame(rect, rects.item(0))
        assertSame(rect, rects[0])
        assertNull(rects[1])
    }

    private class TestDOMRectList(private val rect: DOMRect) : DOMRectList {
        override val length: Int = 1

        override fun item(index: Int): DOMRect? = rect.takeIf { index == 0 }
    }
}
