/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.browser.dom.HTMLDivElement
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Iframe
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Title
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

class ElementLoweringTest {
    @Test
    fun directElementsCallTheBridgeWithoutContentLambdasOrAttributeFields() {
        for (method in listOf("DirectGreeting", "DirectContainer")) {
            val calls = calls(method)
            visitMethod(method, object : MethodVisitor(Opcodes.ASM9) {
                override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
                    assertFalse(name.startsWith("__htmlAttributes"))
                }
            })
            assertTrue(calls.containsAll(setOf("openLoweredRegion", "writeText", "closeElement")), "$method: $calls")
            assertFalse("appendText" in calls, "$method: $calls")
            assertFalse(calls.any {
                it in setOf(
                    "Div",
                    "Span",
                    "Text",
                    "TagElement",
                    "PreparedTextElement",
                    "PreparedContainer"
                )
            })
            assertFalse(calls.any { it.startsWith("composableLambda") })
        }
    }

    @Test
    fun specialTagsComposableValuesAndLabeledReturnsKeepPreparedCalls() {
        for ((method, helper) in mapOf(
            "SpecialText" to "PreparedTextElement",
            "ComposableText" to "PreparedTextElement",
            "RememberedText" to "PreparedTextElement",
            "ContainerReturn" to "PreparedContainer",
        )) {
            assertTrue(helper in calls(method), method)
        }
        assertEquals("<span>composed</span><span>remembered</span><div></div><div>shown</div>", composeHtmlToString {
            ComposableText { "composed" }
            RememberedText("remembered")
            ContainerReturn(true) {}
            ContainerReturn(false) {}
        })
    }

    @Test
    fun containersPreserveEffectsLoopsConditionsAndTextBoundaries() {
        forEachRenderMode { strict, hydratable, stream ->
            val effects = mutableListOf<String>()
            val content: @Composable () -> Unit = {
                Text("before")
                DirectContainer(listOf("<&", "", "é😀"), effects::add)
                DirectGreeting {
                    effects.add("greeting")
                    "dynamic<&"
                }
                Text("after")
                Text("adjacent")
            }
            val boundary = if (hydratable) "<!--c-->" else ""
            val expected = "before<div class=\"container\"><span>head</span><span>value: &lt;&amp;</span>" +
                    "empty<span>value: é😀</span>footer<span>middle</span><span>sibling</span><span>tail</span></div>" +
                    "<div id=\"greeting\" title=\"Hello &amp; goodbye\" class=\"card\">dynamic&lt;&amp;</div>" +
                    "after${boundary}adjacent"
            val actual = render(strict, hydratable, stream, content)
            assertEquals(expected, actual)
            assertEquals(listOf("open", "footer", "close", "greeting"), effects)
        }
    }

    @Test
    fun staticChildrenMergeIntoOpeningAndClosingTags() {
        val constants = methodStrings("DirectSamplesKt", "DirectContainer")
        assertTrue("<div class=\"container\"><span>head</span>" in constants)
        assertTrue("<span>tail</span></div>" in constants)
        assertTrue("<span>middle</span><span>sibling</span>" in constants)
    }

    private fun calls(method: String): Set<String> = methodCalls("DirectSamplesKt", method).toSet()

    private fun visitMethod(method: String, visitor: MethodVisitor) =
        visitSampleMethod("DirectSamplesKt", method, visitor)

    @Test
    fun selectsExpectedLoweringPathsAndRetainsUnsupportedAttributes() {
        val expected = mapOf(
            "PreparedAttributes" to "getCallback",
            "PreparedContainer" to "PreparedContainer",
            "PreparedDynamicText" to "writeText",
            "PreparedGetter" to "writeText",
            "OversizedLiteral" to "beginStaticHtmlFragment",
            "ContentReturn" to "PreparedContainer",
        )
        for ((method, helper) in expected) {
            val calls = runtimeCalls(method)
            assertTrue(helper in calls, "$method: $calls")
            if (method != "PreparedAttributes") {
                assertFalse("Span" in calls || "Div" in calls, "$method: $calls")
            }
        }
        assertTrue("finishMixedElement" in runtimeCalls("AttributeEffects"))
        assertEquals(
            setOf("<span class=\"label\">&lt;&amp;&#13;😀</span>"),
            staticHtmlConstants("PreparedSamplesKt", "PreparedLiteralText"),
        )
    }

    @Test
    fun oversizedLiteralsStreamInChunksAndFieldNamesDoNotCollide() {
        assertEquals("authored", __htmlAttributes0)
        val expected = "<span>" + "&lt;".repeat(2_000) + "</span>"
        forEachRenderMode { strict, hydratable, stream ->
            assertEquals(expected, render(strict, hydratable, stream) { OversizedLiteral() })
        }
    }

    @Test
    fun gettersRunOncePerRenderAndNeverDuringInitialization() {
        var calls = 0
        val user = PreparedUser { "user-${++calls}" }
        assertEquals(0, calls)
        repeat(2) { render ->
            assertEquals("<span class=\"user\">user-${render + 1}</span>", composeHtmlToString {
                PreparedGetter(user)
            })
            assertEquals(render + 1, calls)
        }
    }

    @Test
    fun preparedAttributesSupportStoredNullableAndEffectfulContent() {
        val effects = mutableListOf<String>()
        fun content(): ContentBuilder<HTMLDivElement> {
            effects.add("argument")
            return {
                effects.add("content")
                Text("stored")
            }
        }
        repeat(2) {
            assertEquals("<div title=\"A &amp; B\" class=\"card\">stored</div>", composeHtmlToString {
                PreparedAttributes(content())
            })
        }
        assertEquals(listOf("argument", "content", "argument", "content"), effects)
        assertEquals("<div title=\"A &amp; B\" class=\"card\"></div>", composeHtmlToString {
            PreparedAttributes(null)
        })
    }

    @Test
    fun containersAndTextRetainOrderAcrossRendersAndStreams() {
        forEachRenderMode { strict, hydratable, stream ->
            val effects = mutableListOf<String>()
            repeat(2) { render ->
                val content: @Composable () -> Unit = {
                    Text("before")
                    PreparedContainer("user-$render", effects::add)
                    PreparedDynamicText {
                        effects.add("price")
                        "price-$render"
                    }
                    PreparedLiteralText()
                    Text("after")
                }
                val expected = "before<div class=\"card\">user-$render<span class=\"details\">details</span>end</div>" +
                        "<span class=\"price\">price-$render</span><span class=\"label\">&lt;&amp;&#13;😀</span>after"
                assertEquals(expected, render(strict, hydratable, stream, content))
                assertEquals(List(render + 1) { listOf("container", "text", "price") }.flatten(), effects)
            }
        }
    }

    @Test
    fun earlyReturnsAndInvalidParentsKeepEvaluationOrder() {
        assertEquals("<span class=\"return\"></span>", composeHtmlToString { ContentReturn(true) })
        assertEquals("<span class=\"return\">shown</span>", composeHtmlToString { ContentReturn(false) })
        for (stream in listOf(false, true)) {
            val effects = mutableListOf<String>()
            assertFailsWith<IllegalArgumentException> {
                render(strict = true, hydratable = true, stream = stream) {
                    Table {
                        Span {
                            Text(run {
                                effects.add("text")
                                "invalid child"
                            })
                        }
                    }
                }
            }
            assertTrue(effects.isEmpty())
            assertFailsWith<IllegalStateException> {
                render(strict = true, hydratable = true, stream = stream) {
                    Div {
                        effects.add("before")
                        PreparedDynamicText {
                            effects.add("text")
                            error("text failure")
                        }
                        effects.add("after")
                    }
                }
            }
            assertEquals(listOf("before", "text"), effects)
        }
    }

    @Test
    fun textModesAndRuntimeValidationArePreserved() {
        for (stream in listOf(false, true)) {
            assertEquals(
                "<pre>\n\n&lt;&amp;</pre><title>&lt;&amp;</title><iframe><&</iframe><span></span>",
                render(strict = true, hydratable = true, stream = stream) {
                    Pre { Text("\n<&") }
                    Title { Text("<&") }
                    Iframe { Text("<&") }
                    Span { Text("") }
                })
            for (value in listOf("\n<&", "")) {
                assertEquals(
                    "<pre>${if (value.isEmpty()) "" else "\n\n&lt;&amp;"}</pre>",
                    render(strict = true, hydratable = true, stream = stream) { Pre { Text(value) } })
            }
            assertFailsWith<IllegalArgumentException> {
                render(strict = true, hydratable = true, stream = stream) { PreparedDynamicText { "bad\u0000" } }
            }
        }
    }

    private fun runtimeCalls(method: String): Set<String> =
        methodCalls("PreparedSamplesKt", method, "org/jetbrains/compose/web/dom/").toSet()
}
