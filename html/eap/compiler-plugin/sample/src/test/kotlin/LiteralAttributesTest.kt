/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import kotlinx.browser.dom.HTMLSpanElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

class LiteralAttributesTest {
    @Test
    fun literalsPreserveEscapingAndAttributeOrder() {
        for (hydratable in listOf(false, true)) {
            val ordinary = composeHtmlToString(hydratable = hydratable) {
                RuntimeAttributesPage("\"<&>\r\n\t😀")
            }
            val lowered = composeHtmlToString(hydratable = hydratable) { LiteralAttributesPage() }
            assertEquals(
                "<span id=\"greeting\" title=\"&quot;&lt;&amp;&gt;&#13;&#10;&#9;😀\"" +
                    " data-key=\"value\" aria-label=\"Hello\" role=\"note\"" +
                    " class=\"label accent last\">text</span>",
                lowered
            )
            assertEquals(ordinary, lowered)
            assertNotSame(lowered, composeHtmlToString(hydratable = hydratable) { LiteralAttributesPage() })
        }
    }

    @Test
    fun dynamicAttributesRunOnEveryRender() {
        var calls = 0
        repeat(3) {
            val html = composeHtmlToString {
                DynamicAttributesPage {
                    calls++
                    "value-$calls"
                }
            }
            assertEquals(it + 1, calls)
            assertEquals("<span id=\"value-$calls\">text</span>", html)
        }
    }

    @Test
    fun unsupportedLiteralPatternsKeepRuntimeSemantics() {
        assertEquals(
            "<span id=\"last\" class=\"label label\">text</span>",
            composeHtmlToString { DuplicateAttributesPage() }
        )
        assertEquals(
            "<span class=\"first last\">text</span>",
            composeHtmlToString { SpreadClassesPage() }
        )
        assertEquals(
            "<span class=\"two words\">text</span>",
            composeHtmlToString { InvalidClassPage() }
        )
    }

    @Test
    fun explicitClassOverridesTheClassListInStandaloneComposables() {
        val html = composeHtmlToString { ExplicitClassPage() }
        assertEquals("<span class=\"explicit\">text</span>", html)
        assertNotSame(html, composeHtmlToString { ExplicitClassPage() })
    }

    @Test
    fun callsOnAnotherAttributeReceiverKeepTheirEffects() {
        repeat(2) {
            var assignedId: String? = null
            val target = object : AttrsScopeBuilder<HTMLSpanElement>() {
                override fun attr(attr: String, value: String): AttrsScope<HTMLSpanElement> {
                    assertEquals("id", attr)
                    assignedId = value
                    return this
                }
            }
            assertEquals("<span>text</span>", composeHtmlToString { CapturedAttributeReceiverPage(target) })
            assertEquals("changed", assignedId)
        }
    }

    @Test
    fun invalidAttributesFailDuringRendering() {
        // Other pages in the same source file must still be usable.
        composeHtmlToString { LiteralAttributesPage() }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(validateStrictly = true) { InvalidClassPage() }
        }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(validateStrictly = true) { DuplicateAttributesPage() }
        }
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { InvalidAttributeValuePage() } }
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { ReservedAttributePage() } }
    }
}
