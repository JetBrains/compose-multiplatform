// Verifies the promoted inline-style API on the JVM facade target.
package kotlinx.browser.dom.css.surfaces

import kotlinx.browser.dom.css.CSS
import kotlinx.browser.dom.css.CSSRule
import kotlinx.browser.dom.css.CSSStyleDeclaration
import kotlinx.browser.dom.css.CSSStyleRule
import kotlinx.browser.dom.css.ElementCSSInlineStyle
import kotlinx.browser.toKotlinString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GeneratedCssStyleDeclarationJvmTest {
    @Test
    fun typedAndStringStyleApisShareOneDeclarationState() {
        val style = StubInlineStyleElement().style

        assertEquals(0, style.length)
        style.color = "red"
        assertEquals("red", style.getPropertyValue("color"))
        assertEquals("color", style.item(0).toKotlinString())
        assertEquals("color", style.item(0).toString())

        style.setProperty("background-color", "black")
        assertEquals("black", style.backgroundColor)

        style.setProperty("color", "white", "important")
        assertEquals("white", style.color)
        assertEquals("important", style.getPropertyPriority("color"))

        style.setPropertyValue("display", "block")
        style.setPropertyPriority("display", "important")
        assertEquals("block", style.display)
        assertEquals("important", style.getPropertyPriority("display"))
        assertEquals(3, style.length)

        assertEquals("white", style.removeProperty("color"))
        assertEquals("", style.color)
        assertEquals("", style.getPropertyValue("color"))
        assertEquals("", style.getPropertyPriority("color"))
        assertEquals(2, style.length)

        style.backgroundColor = ""
        assertEquals("", style.getPropertyValue("background-color"))
        assertEquals(1, style.length)
    }

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

private class StubInlineStyleElement : ElementCSSInlineStyle {
    override val style: CSSStyleDeclaration = object : CSSStyleDeclaration() {}
}
