// Verifies the remaining CSS API behavior on the JVM facade target.
package kotlinx.browser.dom.css.surfaces

import kotlinx.browser.dom.css.CSS
import kotlinx.browser.dom.css.CSSRule
import kotlinx.browser.dom.css.CSSStyleRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GeneratedCssStyleDeclarationJvmTest {
    @Test
    fun cssRuleConstantsAreDeterministicAndConsistent() {
        assertEquals(CSSRule.STYLE_RULE, CSSStyleRule.STYLE_RULE)
        assertEquals(CSSRule.NAMESPACE_RULE, CSSStyleRule.NAMESPACE_RULE)
        assertNotEquals(CSSRule.STYLE_RULE, CSSRule.NAMESPACE_RULE)
    }

    @Test
    fun cssEscapeHasAnInertTypeCorrectBody() {
        assertEquals("", CSS.escape("a b"))
    }
}
