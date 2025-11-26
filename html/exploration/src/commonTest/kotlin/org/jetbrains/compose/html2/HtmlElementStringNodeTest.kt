package org.jetbrains.compose.html2

import org.jetbrains.compose.html2.internal.HtmlElementStringNode
import org.jetbrains.compose.html2.internal.HtmlTextStringNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlElementStringNodeTest {

    @Test
    fun testSelfClosingWhenNoChildren() {
        val n = HtmlElementStringNode("div")
        assertEquals("<div></div>", n.toHtmlString())
    }

    @Test
    fun testClassAndStyleAttrsAreIgnoredUnlessViaApi() {
        val n = HtmlElementStringNode("div")
        n.attr("class", "x y")
        n.attr("style", "color:red")
        // Direct class/style attrs are ignored in serialization; no attrs expected
        assertEquals("<div></div>", n.toHtmlString())

        // Now set via APIs; direct attrs still ignored
        n.addClass("a", "b")
        n.setStyle("color", "red")
        n.attr("class", "x y")
        n.attr("style", "background:blue")
        assertEquals("<div class=\"a b\" style=\"color: red\"></div>", n.toHtmlString())
    }

    @Test
    fun testBooleanAttrToggleAndGetAttrContract() {
        val n = HtmlElementStringNode("button")
        n.attr("disabled", true)
        assertTrue(n.hasAttr("disabled"))
        assertEquals("", n.getAttr("disabled"))
        // Idempotent
        n.attr("disabled", true)
        assertTrue(n.hasAttr("disabled"))
        // Turning off removes it
        n.attr("disabled", false)
        assertFalse(n.hasAttr("disabled"))
        assertEquals(null, n.getAttr("disabled"))
    }

    @Test
    fun testDeterministicOrderingUnderMutations() {
        val n = HtmlElementStringNode("div")
        n.addClass("c1")
        n.setStyle("s1", "v1")
        n.attr("a", "1")
        n.attr("b", "2")
        assertEquals("<div class=\"c1\" style=\"s1: v1\" a=\"1\" b=\"2\"></div>", n.toHtmlString())

        // Remove first attr, add new one; order of remaining is preserved, new goes to the end
        n.removeAttr("a")
        n.attr("c", "3")
        assertEquals("<div class=\"c1\" style=\"s1: v1\" b=\"2\" c=\"3\"></div>", n.toHtmlString())

        // Update style value preserves position; remove + re-add moves to the end
        n.setStyle("s1", "v2")
        assertEquals("<div class=\"c1\" style=\"s1: v2\" b=\"2\" c=\"3\"></div>", n.toHtmlString())
        n.removeStyle("s1")
        n.setStyle("s1", "v3")
        assertEquals("<div class=\"c1\" style=\"s1: v3\" b=\"2\" c=\"3\"></div>", n.toHtmlString())

        // Class remove+readd moves to end of class list ordering
        n.addClass("c2")
        n.removeClass("c1")
        n.addClass("c1")
        assertEquals("<div class=\"c2 c1\" style=\"s1: v3\" b=\"2\" c=\"3\"></div>", n.toHtmlString())
    }

    @Test
    fun testNamespacedTagsAndAttributes() {
        val svg = HtmlElementStringNode("svg")
        svg.attr("xml:lang", "en")
        val use = HtmlElementStringNode("use")
        use.attr("xlink:href", "#icon")
        svg.appendElement(use)
        assertEquals("<svg xml:lang=\"en\"><use xlink:href=\"#icon\"></use></svg>", svg.toHtmlString())
    }

    @Test
    fun testEscapingTabsNewlinesInAttributesAndStyles() {
        val n = HtmlElementStringNode("div")
        n.attr("title", "a\tb\nc\r\" & < >")
        n.setStyle("k\t\n\r<>&", "v\t\n\r<>&")
        assertEquals(
            "<div style=\"k&#9;&#10;&#13;&amp;lt;&amp;gt;&amp;amp;: v&#9;&#10;&#13;&amp;lt;&amp;gt;&amp;amp;\" title=\"a&#9;b&#10;c&#13;&quot; &amp; &lt; &gt;\"></div>",
            n.toHtmlString()
        )
    }

    @Test
    fun testInnerTextNullAndOverrideBehavior() {
        val div = HtmlElementStringNode("div")
        div.appendText("x")
        div.appendText("y")
        // Setting innerText overrides existing children
        div.innerText = "z"
        assertEquals("<div>z</div>", div.toHtmlString())
        // Setting null clears children and yields empty element
        div.innerText = null
        assertEquals("<div></div>", div.toHtmlString())

        val input = HtmlElementStringNode("input")
        input.innerText = null
        assertEquals("<input>", input.toHtmlString())
    }

    @Test
    fun testRemoveByChildAndReplaceAt() {
        val p = HtmlElementStringNode("p")
        val t1 = HtmlElementStringNode.Child.Text(HtmlTextStringNode("A"))
        val em = HtmlElementStringNode("em").also { it.innerText = "B" }
        val e1 = HtmlElementStringNode.Child.Element(em)
        val t2 = HtmlElementStringNode.Child.Text(HtmlTextStringNode("C"))
        p.append(t1)
        p.append(e1)
        p.append(t2)
        assertEquals("<p>A<em>B</em>C</p>", p.toHtmlString())

        // Remove text child by value
        assertTrue(p.remove(t1))
        assertFalse(p.remove(t1))
        assertEquals("<p><em>B</em>C</p>", p.toHtmlString())

        // Replace element at index 0
        val strong = HtmlElementStringNode("strong").also { it.innerText = "X" }
        val prev = p.replaceAt(0, HtmlElementStringNode.Child.Element(strong))
        assertTrue(prev is HtmlElementStringNode.Child.Element)
        assertEquals("<p><strong>X</strong>C</p>", p.toHtmlString())
    }

    @Test
    fun testStandaloneTextNodeSerialization() {
        val t = HtmlTextStringNode("Fish & Chips <3>")
        assertEquals("Fish &amp; Chips &lt;3&gt;", t.toHtmlString())
    }

    @Test
    fun testStandaloneTextNodeReuseAcrossElements() {
        val t = HtmlTextStringNode("X")
        val a = HtmlElementStringNode("span").also { it.append(HtmlElementStringNode.Child.Text(t)) }
        val b = HtmlElementStringNode("div").also { it.append(HtmlElementStringNode.Child.Text(t)) }
        assertEquals("<span>X</span>", a.toHtmlString())
        assertEquals("<div>X</div>", b.toHtmlString())
        // mutate text and ensure both reflect updated text since node is shared
        t.text = "Y"
        assertEquals("<span>Y</span>", a.toHtmlString())
        assertEquals("<div>Y</div>", b.toHtmlString())
    }

    @Test
    fun testIdentityEqualitySemantics() {
        val a = HtmlElementStringNode("div").also { it.addClass("x"); it.attr("title", "t") }
        val b = HtmlElementStringNode("div").also { it.addClass("x"); it.attr("title", "t") }
        // Reference inequality expected even if structure matches
        assertFalse(a === b && true) // trivial sanity
        // ensure not accidentally equal via data equality (no overrides exist)
        assertFalse(a == b)
    }

    @Test
    fun idInteropWithAttr() {
        val n = HtmlElementStringNode("div")
        // set via property
        n.id = "main"
        assertEquals("main", n.getAttr("id"))
        assertEquals("<div id=\"main\"></div>", n.toHtmlString())
        // overwrite via attr API
        n.attr("id", "root")
        assertEquals("root", n.id)
        assertEquals("<div id=\"root\"></div>", n.toHtmlString())
        // remove via property
        n.id = null
        assertEquals(null, n.getAttr("id"))
        assertEquals("<div></div>", n.toHtmlString())
    }

    @Test
    fun attrOverwriteNoDuplicates() {
        val n = HtmlElementStringNode("div")
        n.attr("a", "1")
        n.attr("b", true)
        assertEquals("<div a=\"1\" b></div>", n.toHtmlString())
        // overwrite string value keeps single attr
        n.attr("a", "2")
        assertEquals("<div a=\"2\" b></div>", n.toHtmlString())
        // switch boolean->string
        n.attr("b", "yes")
        assertEquals("<div a=\"2\" b=\"yes\"></div>", n.toHtmlString())
        // switch string->boolean
        n.attr("a", true)
        assertEquals("<div a b=\"yes\"></div>", n.toHtmlString())
    }

    @Test
    fun childrenMutationBounds() {
        val n = HtmlElementStringNode("div")
        // insert out of bounds
        try {
            n.insert(-1, HtmlElementStringNode.Child.Element(HtmlElementStringNode("span")))
            assertFalse(true)
        } catch (_: IndexOutOfBoundsException) {}
        try {
            n.insert(1, HtmlElementStringNode.Child.Element(HtmlElementStringNode("span")))
            assertFalse(true)
        } catch (_: IndexOutOfBoundsException) {}
        // removeAt out of bounds
        try {
            n.removeAt(0)
            assertFalse(true)
        } catch (_: IndexOutOfBoundsException) {}
        // replaceAt out of bounds
        try {
            n.replaceAt(0, HtmlElementStringNode.Child.Element(HtmlElementStringNode("span")))
            assertFalse(true)
        } catch (_: IndexOutOfBoundsException) {}
    }

    @Test
    fun voidElementWithChildrenPermissive() {
        val input = HtmlElementStringNode("input")
        input.appendText("x")
        assertEquals("<input>x</input>", input.toHtmlString())
        val img = HtmlElementStringNode("img")
        val span = HtmlElementStringNode("span").also { it.innerText = "y" }
        img.appendElement(span)
        assertEquals("<img><span>y</span></img>", img.toHtmlString())
    }

    @Test
    fun textDoesNotRecognizeEntities() {
        val p = HtmlElementStringNode("p")
        p.appendText("Fish &amp; Chips")
        // ampersand in source is escaped; existing &amp; becomes &amp;amp;
        assertEquals("<p>Fish &amp;amp; Chips</p>", p.toHtmlString())
    }

    @Test
    fun quotesEscapingContracts() {
        val span = HtmlElementStringNode("span")
        span.attr("title", "\"quoted\"")
        span.appendText("\"quoted\"")
        assertEquals("<span title=\"&quot;quoted&quot;\">\"quoted\"</span>", span.toHtmlString())
    }

    @Test
    fun attributeNamesCaseAndNamespaces() {
        val n = HtmlElementStringNode("div")
        n.attr("DATA-ID", "A")
        n.attr("data-test", "B")
        n.attr("foo:bar", "C")
        assertEquals("<div DATA-ID=\"A\" data-test=\"B\" foo:bar=\"C\"></div>", n.toHtmlString())
    }

    @Test
    fun classesDuplicatesAndForcedToggle() {
        val n = HtmlElementStringNode("div")
        n.addClass("a", "a", "b")
        assertEquals("<div class=\"a b\"></div>", n.toHtmlString())
        // force true keeps present
        assertTrue(n.toggleClass("a", force = true))
        assertEquals("<div class=\"a b\"></div>", n.toHtmlString())
        // force false removes
        assertFalse(n.toggleClass("a", force = false))
        assertEquals("<div class=\"b\"></div>", n.toHtmlString())
    }

    @Test
    fun innerTextGetterWithMultipleChildren() {
        val n = HtmlElementStringNode("p")
        n.appendText("a")
        n.appendText("b")
        assertEquals(null, n.innerText)
        val m = HtmlElementStringNode("p")
        m.appendElement(HtmlElementStringNode("em").also { it.innerText = "x" })
        assertEquals(null, m.innerText)
    }

    @Test
    fun deepTreeSmokeTest() {
        var node = HtmlElementStringNode("div")
        for (i in 0 until 100) {
            val child = HtmlElementStringNode("div")
            child.appendText("#$i")
            node.appendElement(child)
            node.appendText("|")
        }
        val html = node.toHtmlString()
        // basic sanity: starts/ends with wrapper and contains a known mid marker
        assertTrue(html.startsWith("<div>"))
        assertTrue(html.endsWith("</div>"))
        assertTrue(html.contains("<div>#50</div>"))
    }

    @Test
    fun stylesInterleavingWithAttrs() {
        val n = HtmlElementStringNode("div")
        n.attr("a", "1")
        n.setStyle("s1", "v1")
        n.attr("b", "2")
        n.setStyle("s2", "v2")
        assertEquals("<div style=\"s1: v1; s2: v2\" a=\"1\" b=\"2\"></div>", n.toHtmlString())
        // remove and re-add style moves to end
        n.removeStyle("s1")
        n.setStyle("s1", "v3")
        assertEquals("<div style=\"s2: v2; s1: v3\" a=\"1\" b=\"2\"></div>", n.toHtmlString())
    }

    @Test
    fun mixedBooleanAndStringAttrOrdering() {
        val n = HtmlElementStringNode("div")
        n.attr("a", true)
        n.attr("b", "x")
        n.attr("c", true)
        assertEquals("<div a b=\"x\" c></div>", n.toHtmlString())
    }

    @Test
    fun testAttributesStringAndBoolean() {
        val n = HtmlElementStringNode("input")
        n.attr("type", "checkbox")
        n.attr("disabled", true)
        assertEquals("<input type=\"checkbox\" disabled>", n.toHtmlString())
    }

    @Test
    fun testClassesAndIdAndStyles() {
        val n = HtmlElementStringNode("div")
        n.id = "main"
        n.addClass("a", "b")
        n.setStyle("color", "red")
        n.setStyle("background", "white")
        val html = n.toHtmlString()
        // Order: class, style, then other attrs (id). Styles keep insertion order with '; '
        assertEquals("<div class=\"a b\" style=\"color: red; background: white\" id=\"main\"></div>", html)
    }

    @Test
    fun testMixedChildrenSerialization() {
        val n = HtmlElementStringNode("p")
        n.appendText("Hello ")
        val b = HtmlElementStringNode("b")
        b.appendText("world")
        n.appendElement(b)
        n.appendText("!")
        assertEquals("<p>Hello <b>world</b>!</p>", n.toHtmlString())
    }

    @Test
    fun testEscapingInTextAndAttributes() {
        val n = HtmlElementStringNode("span")
        n.attr("title", "5 < 6 & 7 > 3 \"quote\"")
        n.appendText("5 < 6 & 7 > 3")
        assertEquals("<span title=\"5 &lt; 6 &amp; 7 &gt; 3 &quot;quote&quot;\">5 &lt; 6 &amp; 7 &gt; 3</span>", n.toHtmlString())
    }

    @Test
    fun testInnerTextConvenience() {
        val n = HtmlElementStringNode("div")
        n.innerText = "hello"
        assertEquals("<div>hello</div>", n.toHtmlString())
        // replacing
        n.innerText = "world"
        assertEquals("<div>world</div>", n.toHtmlString())
    }

    @Test
    fun testChildrenMutationHelpers() {
        val ul = HtmlElementStringNode("ul")
        val li1 = HtmlElementStringNode("li").also { it.innerText = "one" }
        val li2 = HtmlElementStringNode("li").also { it.innerText = "two" }
        ul.appendElement(li1)
        ul.insert(0, HtmlElementStringNode.Child.Element(li2))
        assertEquals("<ul><li>two</li><li>one</li></ul>", ul.toHtmlString())

        val removed = ul.removeAt(0)
        assertTrue(removed is HtmlElementStringNode.Child.Element)
        assertEquals("<ul><li>one</li></ul>", ul.toHtmlString())

        val li3 = HtmlElementStringNode("li").also { it.innerText = "three" }
        ul.replaceAt(0, HtmlElementStringNode.Child.Element(li3))
        assertEquals("<ul><li>three</li></ul>", ul.toHtmlString())
    }

    @Test
    fun testToggleClass() {
        val n = HtmlElementStringNode("div")
        assertTrue(n.toggleClass("x"))
        assertTrue(n.hasClass("x"))
        assertFalse(n.toggleClass("x"))
        assertFalse(n.hasClass("x"))
    }
}
