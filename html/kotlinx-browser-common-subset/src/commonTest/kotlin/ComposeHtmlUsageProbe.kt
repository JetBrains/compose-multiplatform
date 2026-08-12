// Mimics DOM operations a Compose HTML renderer might perform.
package kotlinx.browser.dom.probe

import kotlinx.browser.dom.Document
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.HTMLInputElement
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.Text
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import kotlinx.browser.dom.events.MouseEvent
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal fun renderTree(document: Document): Element {
    val root = document.createElement("div")
    root.setAttribute("id", "root")
    root.setAttribute("class", "container")

    val heading = document.createElement("h1")
    val headingText: Text = document.createTextNode("Hello")
    heading.appendChild(headingText)
    root.appendChild(heading)

    val paragraph = document.createElement("p")
    paragraph.appendChild(document.createTextNode("Body copy"))
    root.appendChild(paragraph)

    return root
}

internal fun styleElement(document: Document) {
    val html = document.createElement("div") as? HTMLElement ?: return
    html.style.setProperty("color", "red")
    html.style.removeProperty("color")
}

internal fun attachListener(document: Document): Int {
    val target: EventTarget = document.createElement("button")
    var clicks = 0
    val listener: (Event) -> Unit = { event ->
        val mouse = event as? MouseEvent
        clicks += if (mouse != null) 2 else 1
    }
    target.addEventListener("click", listener)
    target.removeEventListener("click", listener)
    return clicks
}

internal fun mutateStructure(document: Document) {
    val parent: Node = document.createElement("div")
    val first = document.createElement("span")
    val second = document.createElement("span")

    parent.appendChild(first)
    parent.insertBefore(second, first)
    parent.replaceChild(first, second)
    parent.removeChild(first)
}

internal fun exerciseComposeHtmlUsage(document: Document) {
    val root = renderTree(document)
    assertTrue(root.childNodes.length >= 0)
    root.removeAttribute("class")

    styleElement(document)
    assertEquals(0, attachListener(document))

    val typed = document.createElement("input") as? HTMLInputElement
    if (typed != null) {
        typed.value = "typed text"
        typed.checked = true
        assertEquals("typed text", typed.value)
        assertTrue(typed.checked)
    }

    mutateStructure(document)
}
