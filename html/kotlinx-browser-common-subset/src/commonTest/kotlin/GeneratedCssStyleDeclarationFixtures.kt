// Provides CSS fixtures for browser runtime tests.
package kotlinx.browser.dom.css.surfaces

import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.css.CSS
import kotlinx.browser.dom.css.CSSStyleDeclaration
import kotlinx.browser.dom.css.get

private fun useInlineStyle(element: HTMLElement): String? {
    val style: CSSStyleDeclaration = element.style
    style.color = "rebeccapurple"
    style.display = "block"
    style.setProperty("background-color", "black")
    style.setProperty("color", "white", "important")
    style.getPropertyValue("color")
    style.getPropertyPriority("color")
    style.removeProperty("display")
    return style[0]
}

// Runs only where the browser implements these legacy declarations.
internal fun exerciseGeneratedLegacyCssStyleDeclaration(style: CSSStyleDeclaration) {
    style.setPropertyValue("margin", "0")
    style.setPropertyPriority("margin", "important")
}

private fun escapeCssIdentifier(identifier: String): String = CSS.escape(identifier)

internal fun exerciseGeneratedCssStyleDeclaration(element: HTMLElement) {
    useInlineStyle(element)
    escapeCssIdentifier("portable:id")
}
