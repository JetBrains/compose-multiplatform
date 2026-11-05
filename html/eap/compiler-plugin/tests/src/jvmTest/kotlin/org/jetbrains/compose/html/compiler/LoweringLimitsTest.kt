/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.jetbrains.compose.html.compiler.samples.InvalidLargeRawFragment
import org.jetbrains.compose.html.compiler.samples.LargeDirectAttribute
import org.jetbrains.compose.html.compiler.samples.LargeMixedAttribute
import org.jetbrains.compose.html.compiler.samples.LargeLeadingNewlineFragment
import org.jetbrains.compose.html.compiler.samples.LargeStoredContentAttribute
import org.jetbrains.compose.html.compiler.samples.LargeRawFragment
import org.jetbrains.compose.html.compiler.samples.LargeStaticPage
import org.jetbrains.compose.html.compiler.samples.LargeTableFragment
import org.jetbrains.compose.html.compiler.samples.LargeUnicodeFragment
import org.jetbrains.compose.web.composeHtmlToStream
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderHydratedDocument
import org.jetbrains.compose.web.renderHydratedDocumentToStream
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

class LoweringLimitsTest {
    @Test
    fun smallPagesKeepTheSingleStringFastPath() {
        val page: @Composable () -> Unit = { Html { Body { Text("small") } } }
        val html = composeHtmlToString(content = page)
        assertEquals("<html><body>small</body></html>", html)
        assertSame(html, composeHtmlToString(content = page))
    }

    @Test
    fun literalAppendsBeginEachLogicalFragmentOnceWithoutFieldsOrJoins() {
        val fields = Class.forName("${javaClass.packageName}.samples.LargeFragmentKt").declaredFields
        assertFalse(fields.any { it.name.startsWith("__html") })
        for (method in listOf(
            "LargeUnicodeFragment", "LargeStaticPage", "LargeRawFragment",
            "LargeLeadingNewlineFragment", "LargeTableFragment", "LargeTableRowFragment"
        )) {
            val calls = mutableListOf<String>()
            val literals = mutableListOf<String>()
            sampleBytecode("LargeFragmentKt").accept(object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?
                ): MethodVisitor? {
                    if (name != method) return null
                    return object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitLdcInsn(value: Any) {
                            if (value is String) literals.add(value)
                        }

                        override fun visitMethodInsn(
                            opcode: Int,
                            owner: String,
                            name: String,
                            descriptor: String,
                            isInterface: Boolean
                        ) {
                            calls.add(name)
                        }

                        override fun visitIntInsn(opcode: Int, operand: Int) {
                            assertFalse(opcode == Opcodes.NEWARRAY, method)
                        }

                        override fun visitTypeInsn(opcode: Int, type: String) {
                            assertFalse(opcode == Opcodes.ANEWARRAY, method)
                        }
                    }
                }
            }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            assertEquals(1, calls.count { it == "beginStaticHtmlFragment" }, "$method: $calls")
            assertTrue(calls.count { it == "append" } >= 2, method)
            assertEquals(calls.count { it == "append" }, calls.count { it == "flushChunk\$default" }, method)
            assertFalse(calls.any {
                it in setOf(
                    "toString",
                    "joinToString",
                    "WriteStaticHtml",
                    "appendStaticHtmlChunk"
                )
            }, method)
            assertTrue(literals.all { it.length <= 8_000 }, method)
        }
    }

    @Test
    fun unicodeAndTextStateMatchForStringsAndIndependentlyEncodedStreamChunks() {
        for (strict in listOf(false, true)) {
            for (hydratable in listOf(false, true)) {
                val boundary = "<!--c-->"
                val unicode = "<div>" + "x".repeat(7_994) + "😀&lt;&amp;&#13;${boundary}tail<span>child</span></div>"
                val raw = "<iframe>" + "x".repeat(7_994) + "\n😀\uFFFD<&</iframe>"
                val pre = "<pre>\n\n" + "x".repeat(7_994) + "😀</pre>"
                val table = "<table><tbody><tr><td>" + "x".repeat(7_994) + "😀</td></tr></tbody></table>"
                val cases = listOf<Pair<String, @Composable () -> Unit>>(
                    unicode to { LargeUnicodeFragment() }, raw to { LargeRawFragment() },
                    pre to { LargeLeadingNewlineFragment() }, table to { LargeTableFragment() },
                )
                for ((expected, content) in cases) {
                    assertEquals(
                        expected,
                        composeHtmlToString(validateStrictly = strict, hydratable = hydratable, content = content)
                    )
                    for (chunkSize in listOf(1, 2_048, 8_000, 8_001)) {
                        val bytes = ByteArrayOutputStream()
                        composeHtmlToStream(
                            validateStrictly = strict, hydratable = hydratable, chunkSize = chunkSize,
                            sink = { bytes.write(it.toByteArray(Charsets.UTF_8)) }, content = content
                        )
                        assertEquals(expected, bytes.toString(Charsets.UTF_8), "chunkSize=$chunkSize")
                    }
                    val bytes = ByteArrayOutputStream()
                    OutputStreamWriter(bytes, Charsets.UTF_8).use { writer ->
                        composeHtmlToStream(
                            writer, validateStrictly = strict, hydratable = hydratable,
                            chunkSize = 1, content = content
                        )
                    }
                    assertEquals(expected, bytes.toString(Charsets.UTF_8))
                }
                val outerBoundary = if (hydratable) "<!--c-->" else ""
                assertEquals("head${unicode}after${outerBoundary}tail", composeHtmlToString(hydratable = hydratable) {
                    Text("head")
                    LargeUnicodeFragment()
                    Text("after")
                    Text("tail")
                })
            }
        }
    }

    @Test
    fun largePagesCountAsOneDocumentRootAndStreamWithoutJoining() {
        val expected =
            "<html><body><div>" + ("<span>" + "界".repeat(3_000) + "</span>").repeat(30) + "</div></body></html>"
        assertEquals(expected, composeHtmlToString { LargeStaticPage() })
        assertEquals("<!doctype html>$expected", renderHydratedDocument(validateStrictly = true) { LargeStaticPage() })
        val chunks = mutableListOf<String>()
        renderHydratedDocumentToStream(validateStrictly = true, chunkSize = 1, sink = chunks::add) { LargeStaticPage() }
        assertEquals("<!doctype html>$expected", chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 8_000 })
        for (stream in listOf(false, true)) {
            assertFailsWith<IllegalArgumentException> {
                if (stream) renderHydratedDocumentToStream(validateStrictly = true, sink = {}) {
                    LargeStaticPage()
                    LargeStaticPage()
                } else renderHydratedDocument(validateStrictly = true) {
                    LargeStaticPage()
                    LargeStaticPage()
                }
            }
        }
    }

    @Test
    fun validatesCompleteRawTextAndParentBeforeStreamingTheFragment() {
        for (strict in listOf(false, true)) {
            val chunks = mutableListOf<String>()
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToStream(
                    validateStrictly = strict,
                    chunkSize = 1,
                    sink = chunks::add
                ) { InvalidLargeRawFragment() }
            }
            assertTrue(chunks.isEmpty())
        }
        for (stream in listOf(false, true)) {
            assertFailsWith<IllegalArgumentException> {
                val content: @Composable () -> Unit = { Table { LargeUnicodeFragment() } }
                if (stream) composeHtmlToStream(validateStrictly = true, chunkSize = 1, sink = {}, content = content)
                else composeHtmlToString(validateStrictly = true, content = content)
            }
        }
        assertEquals("<area><span>after</span>", composeHtmlToString {
            Area { LargeUnicodeFragment() }
            Span { Text("after") }
        })
    }

    @Test
    fun largeAttributesRenderWithOriginalLambdasAndCacheMixedLiterals() {
        val title = "界".repeat(24_000)
        val expected = "<span title=\"$title\">value</span>"
        for (content in listOf<@Composable () -> Unit>(
            { LargeDirectAttribute("value") }, { LargeStoredContentAttribute { Text("value") } },
        )) {
            assertEquals(expected, composeHtmlToString(content = content))
            val bytes = ByteArrayOutputStream()
            composeHtmlToStream(
                chunkSize = 1,
                sink = { bytes.write(it.toByteArray(Charsets.UTF_8)) },
                content = content
            )
            assertEquals(expected, bytes.toString(Charsets.UTF_8))
        }
        assertEquals("<span title=\"$title\" id=\"value\">value</span>", composeHtmlToString {
            LargeMixedAttribute("value")
        })
        val fields = Class.forName("${javaClass.packageName}.samples.LargeAttributeKt").declaredFields
            .filter { it.name.startsWith("__htmlString") }
        assertEquals(1, fields.size)
        for (field in fields) {
            assertEquals(String::class.java, field.type)
            assertTrue(
                Modifier.isPrivate(field.modifiers) && Modifier.isStatic(field.modifiers) && Modifier.isFinal(
                    field.modifiers
                )
            )
            field.isAccessible = true
            assertTrue((field.get(null) as String).length >= 24_000)
        }
    }

    @Test
    fun evaluationAndEncodedOutputBudgetsRemainIndependentOfChunkSize() {
        val seed = "xxxxxxxxxxxxxxxx"
        val a = seed + seed
        val b = a + a
        val c = b + b
        val d = c + c
        val e = d + d
        val f = e + e
        val g = f + f
        val h = g + g
        val i = h + h
        val j = i + i
        val k = j + j
        val l = k + k
        val m = l + l
        val n = m + m
        val o = n + n
        val p = o + o
        // More than the evaluation budget: retain ordinary text evaluation.
        assertEquals(1_048_589, composeHtmlToString { Span { Text(p) } }.length)
        // Escaping crosses the encoding budget even though expansion has only 2,000 calls.
        val escaped = composeHtmlToString { Div { repeat(2_000) { Span { Text(EscapingText) } } } }
        assertEquals(2_074_011, escaped.length)
        assertTrue(escaped.startsWith("<div><span>&lt;&lt;"))
    }

    @Test
    fun functionAndFileBudgetsFallBackWithoutChangingOutput() {
        val calls = classMethodCalls(comparison.bytecode("sample.BudgetKt"))
        for (index in 0..8) {
            assertEquals(
                if (index < 8) 64 else 0,
                calls.getValue("BudgetFragment$index").count { it == "beginStaticHtmlFragment" })
        }
        val expected = ("<span>" + "x".repeat(8_001) + "</span>").repeat(65)
        for (index in listOf(0, 7, 8)) {
            val name = "BudgetCase$index"
            comparison.assertEquivalent(listOf(name))
            assertEquals("$expected|events=[]", comparison.ordinary.getValue(name))
        }
    }

    @Test
    fun regionsKeepTheirOuterPlanWhenChildrenExceedBudgets() {
        val calls = classMethodCalls(comparison.bytecode("sample.RegionBudgetKt"))
        for (method in listOf("RegionChunkBudget", "RegionLengthBudget")) {
            val methodCalls = calls.getValue(method)
            val batches = methodCalls.count { it in setOf("openLoweredRegion", "advanceLoweredRegion", "closeElement") }
            assertEquals(1, methodCalls.count { it == "openLoweredRegion" }, method)
            assertEquals(1, methodCalls.count { it == "closeElement" }, method)
            assertTrue(batches in 2..128, "$method: $batches")
            if (method == "RegionChunkBudget") {
                assertEquals(128, batches)
                assertTrue("Span" in methodCalls)
            }
        }
        comparison.assertEquivalent(listOf("ChunkBudget", "LengthBudget"))
        assertEquals(
            "<div>" + "<span>&lt;&amp;</span>".repeat(150) + "</div>|events=[]",
            comparison.ordinary.getValue("ChunkBudget")
        )
        assertEquals(
            "<div>" + ("<span>" + "x".repeat(8_000) + "</span>").repeat(125) + "</div>|events=[]",
            comparison.ordinary.getValue("LengthBudget")
        )
    }

    companion object {
        private val comparison by lazy {
            val imports =
                "package sample\nimport androidx.compose.runtime.Composable\nimport org.jetbrains.compose.web.dom.*\n"
            // LoweringLimits.kt: cross 8_000-char chunks, 128/1_024-chunk method/file caps and the 1_000_000-char fragment cap.
            val fragments = (0..8).joinToString("\n") { index ->
                "@Composable fun BudgetFragment$index() {\n" + "Span { Text(BudgetText) }\n".repeat(65) + "}"
            }
            LoweringComparison(
                mapOf(
                    "BudgetCase0" to "BudgetFragment0()",
                    "BudgetCase7" to "BudgetFragment7()",
                    "BudgetCase8" to "BudgetFragment8()",
                    "ChunkBudget" to "RegionChunkBudget(\"<&\")",
                    "LengthBudget" to "RegionLengthBudget()",
                ),
                matrixCases = emptySet(),
                extraSources = mapOf(
                    "Budget.kt" to imports + "private const val BudgetText = \"${"x".repeat(8_001)}\"\n" + fragments,
                    "RegionBudget.kt" to imports + "private const val RegionText = \"${"x".repeat(8_000)}\"\n" +
                            "@Composable fun RegionChunkBudget(value: String) { Div {\n" +
                            "Span { Text(value) }\n".repeat(150) + "} }\n" +
                            "@Composable fun RegionLengthBudget() { Div {\n" +
                            "Span { Text(RegionText) }\n".repeat(125) + "} }\n",
                ),
            )
        }
    }
}

private const val EscapingText = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" +
        "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" +
        "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" +
        "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
