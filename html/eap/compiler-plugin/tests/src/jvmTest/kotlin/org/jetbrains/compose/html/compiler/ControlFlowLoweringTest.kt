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
import org.jetbrains.compose.html.compiler.samples.ConstantRepeat
import org.jetbrains.compose.html.compiler.samples.EffectfulRepeat
import org.jetbrains.compose.html.compiler.samples.EmptyConstantRepeats
import org.jetbrains.compose.html.compiler.samples.IndexedRepeat
import org.jetbrains.compose.html.compiler.samples.LargeRepeat
import org.jetbrains.compose.html.compiler.samples.LoopCallbacks
import org.jetbrains.compose.html.compiler.samples.LoopRows
import org.jetbrains.compose.html.compiler.samples.LoopTextBoundaries
import org.jetbrains.compose.html.compiler.samples.LoopValidationScope
import org.jetbrains.compose.html.compiler.samples.NestedLoops
import org.jetbrains.compose.html.compiler.samples.ShadowedRepeat
import org.jetbrains.compose.html.compiler.samples.TopLevelForEach
import org.jetbrains.compose.html.compiler.samples.TopLevelLoop
import org.jetbrains.compose.html.compiler.samples.TopLevelLoopText
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

class ControlFlowLoweringTest {
    @Test
    fun loopsReadWriterAndValidationOnce() {
        for (method in listOf("LoopRows", "TopLevelLoop", "TopLevelForEach", "TopLevelLoopText", "NestedLoops")) {
            val calls = mutableListOf<String>()
            val labels = mutableMapOf<Label, Int>()
            val jumps = mutableListOf<Pair<Int, Label>>()
            val contextReads = mutableListOf<Int>()
            var position = 0
            sampleBytecode("LoopRenderingKt").accept(object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?
                ): MethodVisitor? {
                    if (name != method) return null
                    return object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitMethodInsn(
                            opcode: Int,
                            owner: String,
                            name: String,
                            descriptor: String,
                            isInterface: Boolean
                        ) {
                            calls.add(name)
                            if (name in setOf(
                                    "currentHtmlStringWriter",
                                    "getLocalHtmlValidationMode"
                                )
                            ) contextReads.add(position)
                            position++
                        }

                        override fun visitLabel(label: Label) {
                            labels[label] = position++
                        }

                        override fun visitJumpInsn(opcode: Int, label: Label) {
                            jumps.add(position++ to label)
                        }
                    }
                }
            }, 0)
            assertEquals(1, calls.count { it == "currentHtmlStringWriter" }, "$method: $calls")
            assertEquals(1, calls.count { it == "getLocalHtmlValidationMode" }, "$method: $calls")
            assertFalse(
                calls.any { it in setOf("Div", "Span", "Text") },
                "$method: $calls"
            )
            for ((from, label) in jumps) {
                val target = labels.getValue(label)
                if (target < from) assertFalse(
                    contextReads.any { it in target..from },
                    "$method reads context inside a loop"
                )
            }
        }
    }

    @Test
    fun emptyLoopsEscapingAndControlFlowPreserveOutputAndEffects() {
        forEachRenderMode { strict, hydratable, stream ->
            val effects = mutableListOf<String>()
            val content: @Composable () -> Unit = {
                LoopRows(listOf("<&", "", "é😀"))
                TopLevelLoop(emptyList())
                TopLevelForEach(listOf("<&", ""))
                NestedLoops(listOf("", "a", "stop", "unused"), effects::add)
            }
            val expected = "<div><div><span>&lt;&amp;</span><span>&lt;&amp;</span></div>" +
                    "<div><span></span><span></span></div><div><span>é😀</span><span>é😀</span></div></div>" +
                    "<span>0:&lt;&amp;</span><span>1:</span>" +
                    "<div><span>aa</span><span>astop</span><span>aunused</span>" +
                    "<span>aa</span><span>astop</span><span>aunused</span><span>done:3</span></div>"
            val actual = render(strict, hydratable, stream, content)
            assertEquals(expected, actual)
            assertEquals(listOf("", "a", "stop"), effects)
        }
    }

    @Test
    fun storedCallbacksResolveTheWriterOfTheirInvocation() {
        val callbacks = mutableListOf<@Composable () -> Unit>()
        assertEquals("<div><span>&lt;&amp;</span><span>&lt;&amp;</span></div>", composeHtmlToString {
            LoopCallbacks(listOf("<&"), callbacks::add)
        })
        assertEquals("<span>stored:&lt;&amp;</span>", composeHtmlToString(validateStrictly = true) {
            callbacks.single().invoke()
        })
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(validateStrictly = false) { LoopValidationScope(listOf("invalid child")) }
        }
    }

    @Test
    fun onlyBoundedStaticRepeatsFold() {
        val html = "<div>" + "<span>&lt;&amp;</span>".repeat(3) + "</div>"
        assertEquals(html, composeHtmlToString { ConstantRepeat() })
        assertTrue(html in staticHtmlConstants("LoopRenderingKt", "ConstantRepeat"))
        var effects = 0
        assertEquals("<div></div>", composeHtmlToString { EmptyConstantRepeats { effects++ } })
        assertEquals(0, effects)
        assertEquals("<div><span>0</span><span>1</span><span>2</span></div>", composeHtmlToString { IndexedRepeat() })
        assertEquals("<div></div>", composeHtmlToString { EffectfulRepeat { effects++ } })
        assertEquals(2, effects)
        assertEquals("<div>authored:3</div>", composeHtmlToString { ShadowedRepeat { effects++ } })
        assertEquals(3, effects)
        assertEquals(
            "<div>" + "<span>&lt;&amp;</span>".repeat(10_000) + "</div>",
            composeHtmlToString { LargeRepeat() })
        assertTrue(staticHtmlConstants("LoopRenderingKt", "LargeRepeat").none { it.length > 8_000 })
    }

    @Test
    fun adjacentLoopTextRetainsHydrationBoundaries() {
        assertEquals("<div>a&lt;&amp;</div>", composeHtmlToString(hydratable = false) {
            LoopTextBoundaries(listOf("a", "", "<&"))
        })
        assertEquals("<div>a<!--c-->&lt;&amp;</div>", composeHtmlToString(hydratable = true) {
            LoopTextBoundaries(listOf("a", "", "<&"))
        })
        assertEquals("a<!--c-->&lt;&amp;", composeHtmlToString(hydratable = true) {
            TopLevelLoopText(listOf("a", "", "<&"))
        })
    }

    @Test
    fun regionsPreserveRenderingEffectsAndWriterState() {
        comparison.assertEquivalent()
    }

    @Test
    fun aliasesAndBudgetFallbacksEmitExpectedWriterCalls() {
        val calls = comparison.writerCalls
        assertEquals(1, calls.getValue("TextAcrossAliases")["writeText"])
        assertEquals(2, calls.getValue("TextAcrossAliasesToText")["writeText"])
        assertEquals(1, calls.getValue("TextBeforeTrailingAliases")["writeText"])
        assertEquals(1, calls.getValue("TextBeforeAliasesAndBudgetFallback")["writeText"])
        assertEquals(mapOf("openLoweredRegion" to 1, "closeElement" to 1), calls.getValue("DeepConstant"))
        assertEquals(
            mapOf("openLoweredRegion" to 1, "writeText" to 1, "closeElement" to 1),
            calls.getValue("WideSiblings")
        )
        assertEquals(5, calls.getValue("LargeBatches")["advanceLoweredRegion"])
    }

    companion object {
        private val cases = linkedMapOf(
            "TextAcrossAliases" to """
                Div {
                    Text("lead")
                    val alias = "<&"
                    val second = alias + "!"
                    Span {
                        Probe()
                        Text(second)
                    }
                    Probe()
                }
            """.trimIndent(),
            "TextAcrossAliasesToText" to "Div { Text(\"a\"); val alias = \"b\"; Text(alias); Probe() }",
            "TextBeforeTrailingAliases" to "Div { Probe(); Span {}; Text(\"tail\"); val unused = \"alias\" }",
            "TextAcrossEffect" to "Div { Text(\"lead\"); val alias = Mark(\"value\"); Span { Text(alias); Probe() } }",
            "TextBeforeAliasesAndBudgetFallback" to "Div { " + "P { Probe() }; ".repeat(126) +
                    "Text(\"tail\"); val alias = \"value\"; val second = alias + \"!\"; Span { Text(second); Probe() } }",
            "DeepConstant" to "Div { ".repeat(66) + "Text(\"deep\")" + " }".repeat(66),
            "MixedLiteralChildren" to "Div(attrs = { id(Mark(name)) }) { Span { Text(\"prefix\") }; P { Probe() }; Span { Text(\"suffix\") } }",
            "MixedAdjacentText" to """
                Div(attrs = {
                    id(Mark(name))
                }) {
                    Text("first")
                    Text("")
                    Text(name)
                    Text("last")
                    Span {
                        Probe()
                    }
                }
            """.trimIndent(),
            "MixedTable" to "Table(attrs = { id(Mark(name)) }) { Tbody { Tr { Td { Text(name); Probe() } } } }",
            "MultipleRuntimeCalls" to "Div { H1 { Text(\"Title\") }; P { Probe() }; Probe(); Span { Probe() }; Probe() }",
            "StaticTextAroundTags" to "Div { Text(\"lead\"); P { Probe() }; Text(\"mid\"); Span { Text(\"s\") }; Text(\"tail\") }",
            "LargeBatches" to "Div { P { Probe() }; Span { Text(Big) }; P { Probe() }; Span { Text(Big) } }",
            "Namespace" to "TagElementNS<Element>(\"svg\", \"urn:test\", null) { Div { P { Probe() } }; Probe() }",
            "VoidContent" to "TagElement<Element>(\"input\", null) { Div { P { Effect() } } }; Span { Probe() }",
            "InvalidTableParent" to "Table { Div { P { Boom() } } }",
            "ThrowingContent" to "Div { H1 { Text(\"before\") }; P { Boom() }; Effect() }",
            "WideSiblings" to "Div { ${(0 until 300).joinToString("; ") { "Span { Text(\"item $it\") }" }}; Text(name) }",
            "AbsentContent" to "Div(); Div { Span(); Probe(); Br(); Probe() }; Div(attrs = { id(Mark(name)) })",
            "MixedVoid" to """
                Div {
                    Br(attrs = { id(Mark(name)) })
                    Probe()
                    Input(type = InputType.Text, attrs = { id(Mark(name)) })
                    Probe()
                }
            """.trimIndent(),
            "NestedChunkedStatic" to "Div { if (name.isNotEmpty()) { Span { Text(Big) } }; Probe() }",
            "MultipleRoots" to "Div { P { Probe() } }; Div { Span { Probe() } }; Text(name)",
            "SvgBudgetFallback" to "Svg { " + "G { Probe() }; ".repeat(150) + "Circle(1, 2, 3); Scope(this).Circle(1, 2, 3) }",
            "ReceiverCapture" to """
                Div { P { Text(name) }; Keep(this) }
                Div { Keep(this); P { Probe() } }
            """.trimIndent(),
            "SvgRegions" to """
                Div {
                    Svg(viewBox = "0 0 20 20") {
                        Defs { LinearGradient(id = "gradient") { Stop() } }
                        G { Circle(1, 2, 3); Probe() }
                        Probe()
                    }
                    Probe()
                }
            """.trimIndent(),
        )
        private val comparison by lazy {
            LoweringComparison(
                cases, declarations = """
                private const val Big = "${"y".repeat(20_000)}"
                @Composable fun Keep(scope: Any) { Text("scope") }
                @Composable fun Effect() { Mark("effect"); Text("effect") }
                @Composable fun Boom() { Mark("boom"); error("boom") }
                fun Scope(scope: ElementScope<kotlinx.browser.dom.svg.SVGElement>): ElementScope<kotlinx.browser.dom.svg.SVGElement> {
                    Mark("scope")
                    return scope
                }
            """.trimIndent()
            )
        }
    }
}
