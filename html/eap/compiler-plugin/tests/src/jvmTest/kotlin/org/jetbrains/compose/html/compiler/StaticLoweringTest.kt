/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.browser.dom.HTMLSpanElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

class StaticLoweringTest {
    @Test
    fun literalsPreserveEscapingOrderingAndExplicitClasses() {
        assertEquals(
            "<span id=\"greeting\" title=\"&quot;&lt;&amp;&gt;&#13;&#10;&#9;😀\"" +
                " data-key=\"value\" class=\"label accent last\">text</span>",
            composeHtmlToString {
                Span(attrs = {
                    classes("label", "accent")
                    id("greeting")
                    attr("title", "\"<&>\r\n\t😀")
                    classes("last")
                    attr("data-key", "value")
                }) { Text("text") }
            }
        )
        assertEquals("<span class=\"explicit\">text</span>", composeHtmlToString {
            Span(attrs = { classes("ignored"); attr("class", "explicit") }) { Text("text") }
        })
    }

    @Test
    fun unsupportedPatternsKeepRuntimeValidation() {
        val duplicate: @Composable () -> Unit = {
            Span(attrs = { id("first"); id("last"); classes("label", "label") }) { Text("text") }
        }
        val invalidClass: @Composable () -> Unit = { Span(attrs = { classes("two words") }) { Text("text") } }
        assertEquals("<span id=\"last\" class=\"label label\">text</span>", composeHtmlToString(content = duplicate))
        assertEquals("<span class=\"two words\">text</span>", composeHtmlToString(content = invalidClass))
        assertEquals("<span class=\"first last\">text</span>", composeHtmlToString {
            Span(attrs = { classes(*arrayOf("first", "last")) }) { Text("text") }
        })
        val invalid = listOf<@Composable () -> Unit>(
            duplicate,
            invalidClass,
            { Span(attrs = { attr("title", "bad\u0000") }) { Text("text") } },
            { Span(attrs = { attr("data-compose-hydration-root", "bad") }) { Text("text") } },
        )
        for (page in invalid) {
            assertFailsWith<IllegalArgumentException> { composeHtmlToString(validateStrictly = true, content = page) }
        }
    }

    @Test
    fun callsOnAnotherAttributeReceiverKeepTheirEffects() {
        var assignedId: String? = null
        val target = object : AttrsScopeBuilder<HTMLSpanElement>() {
            override fun attr(attr: String, value: String): AttrsScope<HTMLSpanElement> {
                assertEquals("id", attr)
                assignedId = value
                return this
            }
        }
        assertEquals("<span>text</span>", composeHtmlToString {
            Span(attrs = { target.id("changed") }) { Text("text") }
        })
        assertEquals("changed", assignedId)
    }

    @Test
    fun constantsAliasesAndBuiltinExpressionsArePrecomputed() {
        val cases = mapOf(
            "<div id=\"main\" title=\"same!\" class=\"card\"><span>My &lt;application&gt; &amp; friends</span></div>" to ::renderCrossFileConstants,
            "<div title=\"items=12\"><span>value=13</span></div>" to ::renderConstantExpressions,
            "<span title=\"-2147483648:true:42:A:null\">-3:1:8</span>" to ::renderPrimitiveExpressions,
            "<span>false:true</span>" to ::renderShortCircuitConstants,
        )
        for ((expected, render) in cases) assertEquals(expected, render())
        assertEquals(
            cases.keys + setOf("<span>depth</span>"),
            staticHtmlConstants("ConstantSamplesKt"),
        )
    }

    @Test
    fun depthLimitDoesNotPoisonLaterAliasEvaluation() {
        assertEquals("<span id=\"deep\">depth</span><span>depth</span>", renderAfterConstantDepthLimit())
    }

    @Test
    fun gettersMutableAliasesAndEffectsStayAtRuntime() {
        repeat(2) {
            val before = constantRuntimeCalls
            assertEquals("<span title=\"getter-${before + 1}\">getter-${before + 2}</span>", renderRuntimeGetters())
            assertEquals(before + 2, constantRuntimeCalls)
            assertEquals("<span>effect-${before + 3}</span>", renderEffectfulAlias())
            assertEquals(before + 3, constantRuntimeCalls)
        }
        assertEquals("<span>ordinary</span>", renderOrdinaryVal())
        assertEquals("<span class=\"shadow\">shadow</span>", renderShadowedConstant("shadow"))
        assertEquals("<span>changed</span>", renderMutableAlias())
        assertEquals("<span>user</span>", renderUserToString())
        assertFailsWith<ArithmeticException> { renderThrowingExpression() }
    }

    @Test
    fun staticHtmlUsesConstantsAndDirectElementsNeedNoAttributeFields() {
        val staticMethods = listOf("SharedLiteral", "EquivalentLiteral", "DistinctLiteral",
            "SharedFragment", "EquivalentFragment", "FragmentWithoutBoundary")
        val methods = staticMethods + listOf("SharedDynamic", "SharedContainer", "SharedAttributesOtherTag",
            "SameTagOtherType", "OrderedAttributes", "ReorderedAttributes", "FirstClassMetadata", "SecondClassMetadata")
        val reads = methods.associateWith { fieldReads("SharingSamplesKt", it) }
        assertTrue(reads.values.all { it.isEmpty() })

        assertEquals(staticHtmlConstants("SharingSamplesKt", "SharedLiteral"),
            staticHtmlConstants("SharingSamplesKt", "EquivalentLiteral"))
        assertNotEquals(staticHtmlConstants("SharingSamplesKt", "SharedLiteral"),
            staticHtmlConstants("SharingSamplesKt", "DistinctLiteral"))
        assertEquals(staticHtmlConstants("SharingSamplesKt", "SharedFragment"),
            staticHtmlConstants("SharingSamplesKt", "EquivalentFragment"))
        assertNotEquals(staticHtmlConstants("SharingSamplesKt", "SharedFragment"),
            staticHtmlConstants("SharingSamplesKt", "FragmentWithoutBoundary"))

        assertTrue(fieldReads("PreparedSamplesKt", "PreparedSharedAttributes").isEmpty())
        val fields = Class.forName("${javaClass.packageName}.SharingSamplesKt").declaredFields
        assertEquals(0, fields.count { it.name.startsWith("__htmlAttributes") })
    }

    @Test
    fun sharedValuesPreserveInitializationRenderingAndEffects() {
        val literal = "<span class=\"label\">&lt;&amp;&#13;😀</span>"
        assertEquals(literal.repeat(2), sharedInitialization)
        forEachRenderMode { strict, hydratable, stream ->
            var calls = 0
            repeat(2) {
                val content: @Composable () -> Unit = {
                    SharedLiteral()
                    EquivalentLiteral()
                    PreparedLiteralText()
                    SharedDynamic { "value-${++calls}" }
                    SharedContainer("body")
                    SharedFragment()
                    EquivalentFragment()
                    FragmentWithoutBoundary()
                }
                val boundary = if (hydratable) "<!--c-->" else ""
                val fragmentBoundary = "<!--c-->"
                val fragment = "<div><span>&lt;&amp;&#13;😀</span>tail${fragmentBoundary}end</div>"
                val expected = literal.repeat(3) + "<span class=\"label\">value-${it + 1}</span>" +
                    "<span class=\"label\">body${boundary}tail</span>" + fragment.repeat(2) +
                    "<div><span>&lt;&amp;&#13;😀</span>tailend</div>"
                val actual = render(strict, hydratable, stream, content)
                assertEquals(expected, actual)
                assertEquals(it + 1, calls)
            }
        }
        assertEquals("<span data-a=\"a\" data-b=\"b\">text</span><span data-b=\"b\" data-a=\"a\">text</span>",
            composeHtmlToString {
                OrderedAttributes("text")
                ReorderedAttributes("text")
            })
    }

    @Test
    fun staticTreesCompileToInlinedBuilderAppends() {
        var appends = 0
        var bridgeCalls = 0
        sampleBytecode("SharingSamplesKt").accept(object : ClassVisitor(Opcodes.ASM9) {
            override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor =
                object : MethodVisitor(Opcodes.ASM9) {
                    override fun visitMethodInsn(opcode: Int, owner: String, method: String, descriptor: String, isInterface: Boolean) {
                        if (method == "WriteStaticHtml") bridgeCalls++
                        if (name == "SharedFragment" && owner == "java/lang/StringBuilder" && method == "append" &&
                            descriptor == "(Ljava/lang/String;)Ljava/lang/StringBuilder;") appends++
                    }
                }
        }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        assertEquals(1, appends)
        assertEquals(0, bridgeCalls, "The writer bridge should be inlined into the generated code")
    }
}
