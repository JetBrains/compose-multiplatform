/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MixedAttributeLoweringTest {
    @Test
    fun unconditionalLiteralPrefixJoinsOpeningTag() {
        forEachRenderMode { strict, hydration, stream ->
            for (selected in listOf(false, true)) {
                assertEquals(
                    "<div id=\"card\" title=\"&lt;&amp;&quot;\"${if (selected) " hidden=\"\"" else ""} lang=\"en\" style=\"color: red\"></div>",
                    render(strict, hydration, stream) { MixedLiteralPrefix(selected) }
                )
            }
        }
        val strings = methodStrings("MixedSamplesKt", "MixedLiteralPrefix")
        assertTrue("<div id=\"card\" title=\"&lt;&amp;&quot;\"" in strings)
        assertFalse("<div" in strings)
    }

    @Test
    fun mixedValuesConditionsAndClassesPreserveOutputAndEffects() {
        forEachRenderMode { strict, hydration, stream ->
            for (selected in listOf(false, true)) {
                val effects = mutableListOf<String>()
                val actual = render(strict, hydration, stream) {
                    MixedCard("<&é😀", selected) {
                        effects.add(it)
                        "$it<&\"\r"
                    }
                    MixedButton(selected, "<&")
                }
                val optional = if (selected) " aria-current=\"current&lt;&amp;&quot;&#13;\"" else ""
                val disabled = if (selected) " disabled" else ""
                assertEquals(
                    "<div id=\"id&lt;&amp;&quot;&#13;\" title=\"A &amp; B\"$optional data-value=\"data&lt;&amp;&quot;&#13;\" class=\"card &lt;&amp;é😀${if (selected) " selected" else ""}\">body&lt;&amp;\"&#13;</div>" +
                            "<button$disabled aria-disabled=\"$selected\" aria-label=\"&lt;&amp;\" class=\"button\">&lt;&amp;</button>",
                    actual
                )
                assertEquals(
                    if (selected) listOf("id", "current", "data", "body") else listOf("id", "data", "body"),
                    effects
                )
            }
        }
    }

    @Test
    fun overwritesRetainFirstInsertionAndExplicitClassOverridesMerging() {
        for (first in listOf(false, true)) {
            for (explicit in listOf(false, true)) {
                val effects = mutableListOf<String>()
                val actual = composeHtmlToString(validateStrictly = true) {
                    MixedOverwrite(first, explicit, "dynamic") {
                        effects.add(it)
                        it
                    }
                }
                val attributes =
                    if (first) " title=\"last\" aria-label=\"label\"" else " aria-label=\"label\" title=\"last\""
                assertEquals(
                    "<div$attributes class=\"${if (explicit) "class" else "dynamic card"}\">body</div>",
                    actual
                )
                assertEquals(
                    (if (first) listOf("first") else emptyList()) + listOf("label", "last") + if (explicit) listOf(
                        "class"
                    ) else emptyList(), effects
                )
            }
        }
    }

    @Test
    fun invalidClassTokensStillValidateWhenOverridden() {
        for (explicit in listOf(false, true)) {
            for (value in listOf("", "two tokens", "line\nbreak")) {
                assertFailsWith<IllegalArgumentException> {
                    composeHtmlToString(validateStrictly = true) { MixedInvalid(value, explicit) }
                }
                assertEquals(
                    "<div class=\"${if (explicit) "override" else value.replace("\n", "&#10;")}\">body</div>",
                    composeHtmlToString { MixedInvalid(value, explicit) }
                )
            }
        }
    }

    @Test
    fun duplicateClassesPreserveOrderAndStrictValidation() {
        for (stream in listOf(false, true)) {
            for (explicit in listOf(false, true)) {
                val expectedClass = if (explicit) "class" else "card card"
                assertEquals(
                    "<div aria-label=\"label\" title=\"last\" class=\"$expectedClass\">body</div>",
                    render(false, true, stream) { MixedOverwrite(false, explicit, "card") { it } }
                )
                assertFailsWith<IllegalArgumentException> {
                    render(true, true, stream) { MixedOverwrite(false, explicit, "card") { it } }
                }
                assertEquals(
                    "<div id=\"id\" title=\"A &amp; B\" aria-current=\"current\" data-value=\"data\" class=\"card selected selected\">body</div>",
                    render(false, true, stream) { MixedCard("selected", true) { it } }
                )
                assertFailsWith<IllegalArgumentException> {
                    render(true, true, stream) { MixedCard("selected", true) { it } }
                }
            }
        }
    }

    @Test
    fun literalAndDynamicClassesUseTheSameEscapingAndValidation() {
        for (selected in listOf(false, true)) {
            for (stream in listOf(false, true)) {
                assertEquals(
                    "<div class=\"a&lt;&amp;&quot;${if (selected) " b&amp;" else ""} c&lt;&amp;&quot;\">body</div>",
                    render(true, true, stream) { MixedEncodedClasses("c<&\"", selected) }
                )
                assertFailsWith<IllegalArgumentException> {
                    render(true, true, stream) { MixedEncodedClasses("a<&\"", selected) }
                }
            }
        }
    }

    @Test
    fun generatedCodeWritesValuesDirectlyAndCallbacksKeepTheBuilder() {
        for (method in listOf("MixedCard", "MixedOverwrite", "MixedButton", "MixedBranches")) {
            val calls = calls(method)
            assertTrue("writeMixedAttribute" in calls, "$method: $calls")
            assertFalse(calls.any {
                it in setOf(
                    "Div",
                    "Button",
                    "attr",
                    "classes",
                    "TagElement"
                )
            }, "$method: $calls")
        }
        assertTrue("Div" in calls("MixedFallback"))
        assertEquals("<div id=\"callback\" title=\"after\">body</div>", composeHtmlToString {
            MixedFallback {
                id("callback")
                attr("title", "ignored")
            }
        })
    }

    @Test
    fun inputsPreserveConditionalBooleanAttributesAndNullableValues() {
        val variants = listOf(false, true)
        for (value in listOf(null, "<&")) {
            for (required in variants) {
                for (disabled in variants) {
                    for (checked in variants) {
                        for (stream in variants) {
                            val effects = mutableListOf<String>()
                            val actual = render(true, true, stream) {
                                MixedInput(value, required, disabled, checked) {
                                    effects.add(it)
                                    it
                                }
                            }
                            val attributes = (if (value == null) "" else " value=\"&lt;&amp;\"") +
                                    (if (required) " required" else "") +
                                    (if (disabled) " disabled" else "") +
                                    (if (checked) " checked" else "")
                            assertEquals(
                                "<input type=\"${if (checked) "radio" else "text"}\" id=\"id\" name=\"field\"$attributes aria-required=\"$required\">",
                                actual
                            )
                            assertEquals(if (value == null) listOf("id") else listOf("id", value), effects)
                        }
                    }
                }
            }
        }
        assertTrue("writeMixedAttribute" in calls("MixedInput"))
        for (checked in listOf(false, true)) {
            assertEquals(
                "<input type=\"checkbox\"${if (checked) " checked" else ""}>",
                composeHtmlToString { MixedControlledInput(checked) })
        }
        assertTrue("writeMixedAttribute" in calls("MixedControlledInput"))
    }

    @Test
    fun delegatedMemberAttributesPreserveTypeAndAttributeEvaluationOrder() {
        forEachRenderMode { strict, hydration, stream ->
            val effects = mutableListOf<String>()
            assertEquals(
                "<input type=\"text\" title=\"title&lt;&amp;\" lang=\"lang\" inputmode=\"inputmode\">",
                render(strict, hydration, stream) {
                    MixedInputMemberAttributes("<&") {
                        effects.add(it)
                        it
                    }
                }
            )
            assertEquals(listOf("type", "title", "lang", "inputmode"), effects)
        }
        assertTrue("writeMixedAttribute" in calls("MixedInputMemberAttributes"))
    }

    @Test
    fun branchResultsAndLocalAliasesRemainValidIr() {
        for (selected in listOf(false, true)) {
            assertEquals(
                "<div title=\"${if (selected) "value" else "other"}\" aria-label=\"value label\">body</div>",
                composeHtmlToString { MixedBranches("value", selected) })
        }
    }

    @Test
    fun stylesUseRuntimeCssSerializationAndAttributeEscaping() {
        forEachRenderMode { strict, hydration, stream ->
            for (reset in listOf(false, true)) {
                val color = if (reset) "blue" else "red !important; display: none"
                assertEquals(
                    "<span id=\"tt-&lt;&amp;\" class=\"tooltip-text\" style=\"display: none\">tooltip</span>" +
                            "<div style=\"width: 0.25px; opacity: 0.25; color: $color; --label: &lt;&amp;&quot;&#13;\"></div>",
                    render(strict, hydration, stream) {
                        MixedTooltip("tt-<&")
                        MixedFractionalStyle(0.25, reset)
                    }
                )
            }
        }
        for (method in listOf(
            "MixedTooltip",
            "MixedFractionalStyle",
            "MixedStyleBranches",
            "MixedStyleOverride",
            "MixedInputStyle",
            "MixedSelectStyle"
        )) {
            val calls = calls(method)
            assertTrue("writeMixedStyle" in calls, "$method: $calls")
            assertFalse(
                calls.any { it in setOf("Span", "Div", "Input", "Select", "style", "TagElement") },
                "$method: $calls"
            )
        }
    }

    @Test
    fun styleBlocksPreserveOrderAndConditionalExecution() {
        forEachRenderMode { strict, hydration, stream ->
            for (selected in listOf(false, true)) {
                for (value in listOf(null, "label")) {
                    val effects = mutableListOf<String>()
                    val display = if (selected) "; display: none" else ""
                    val variable = if (value == null) "" else "; --label: label"
                    assertEquals(
                        "<div title=\"title\" style=\"color: red; color: blue$display$variable\"></div>",
                        render(strict, hydration, stream) {
                            MixedStyleBranches(selected, value) {
                                effects.add(it)
                                it
                            }
                        }
                    )
                    assertEquals(
                        listOf("red", "title", "blue") +
                                (if (selected) listOf("none") else emptyList()) + listOfNotNull(value),
                        effects
                    )
                }
            }
        }
    }

    @Test
    fun explicitStyleOverridesStillExecuteStyleBlocks() {
        forEachRenderMode { strict, hydration, stream ->
            for (before in listOf(false, true)) {
                for (explicit in listOf(false, true)) {
                    val effects = mutableListOf<String>()
                    assertEquals(
                        "<div style=\"${if (explicit) "override" else "color: red"}\"></div>",
                        render(strict, hydration, stream) {
                            MixedStyleOverride(before, explicit) {
                                effects.add(it)
                                it
                            }
                        }
                    )
                    assertEquals(
                        when {
                            !explicit -> listOf("red")
                            before -> listOf("override", "red")
                            else -> listOf("red", "override")
                        },
                        effects
                    )
                }
            }
        }
    }

    @Test
    fun styleExpressionsSupportSpecializedScopesAndReceiverCaptureFallsBack() {
        forEachRenderMode { strict, hydration, stream ->
            for (empty in listOf(false, true)) {
                val style = if (empty) "" else " style=\"display: none\""
                assertEquals("<input type=\"text\"$style><select$style></select>", render(strict, hydration, stream) {
                    MixedInputStyle { if (!empty) property("display", "none") }
                    MixedSelectStyle { if (!empty) property("display", "none") }
                })
            }
            assertEquals("<div title=\"&lt;&amp;\" style=\"color: red\"></div>", render(strict, hydration, stream) {
                MixedStyleFallback("<&")
            })
        }
        assertTrue("Div" in calls("MixedStyleFallback"))
    }

    private fun calls(method: String): Set<String> = methodCalls("MixedSamplesKt", method).toSet()
}
