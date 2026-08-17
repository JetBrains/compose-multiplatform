package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.HTMLSpanElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ElementBuilderJvmTest {
    @Test
    fun creatingDivElementIsUnsupported() {
        val builder = ElementBuilder.createBuilder<HTMLDivElement>("DIV")

        val exception = assertFailsWith<UnsupportedOperationException> {
            builder.create()
        }

        assertEquals(
            "DOM element creation for <div> is not available on JVM",
            exception.message
        )
    }

    @Test
    fun creatingSpanElementIsUnsupported() {
        val builder = ElementBuilder.createBuilder<HTMLSpanElement>("SPAN")

        val exception = assertFailsWith<UnsupportedOperationException> {
            builder.create()
        }

        assertEquals(
            "DOM element creation for <span> is not available on JVM",
            exception.message
        )
    }

    @Test
    fun customBuilderKeepsFunctionalInterfaceSemantics() {
        val element = object : HTMLDivElement() {}
        val builder = ElementBuilder { element }

        assertSame(element, builder.create())
    }

    @Test
    fun stringBuilderCarriesNormalizedTagName() {
        val builder = StringElementBuilder<HTMLDivElement>("DIV")

        assertEquals("div", builder.tagName)
    }

    @Test
    fun stringBuilderCannotCreateDomElements() {
        val builder = StringElementBuilder<HTMLDivElement>("DIV")

        val exception = assertFailsWith<UnsupportedOperationException> {
            builder.create()
        }

        assertEquals(
            "String element builder for <div> cannot create a DOM element",
            exception.message
        )
    }
}
