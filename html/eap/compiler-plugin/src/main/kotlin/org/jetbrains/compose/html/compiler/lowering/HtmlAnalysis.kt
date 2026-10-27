/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.html.compiler.lowering

import java.util.IdentityHashMap
import org.jetbrains.compose.html.compiler.HtmlSymbols
import org.jetbrains.compose.html.compiler.ir.KotlinRepeat
import org.jetbrains.compose.html.compiler.ir.contentScopeReferences
import org.jetbrains.compose.html.compiler.ir.isAbsent
import org.jetbrains.compose.web.dom.HtmlNamespace
import org.jetbrains.compose.web.dom.appendHydrationTextBoundaryMarker
import org.jetbrains.compose.web.dom.escapeHtml
import org.jetbrains.compose.web.dom.isHtmlLeadingNewlineElement
import org.jetbrains.compose.web.dom.isHtmlParserStableTableElement
import org.jetbrains.compose.web.dom.isHtmlParserStableTableText
import org.jetbrains.compose.web.dom.isHtmlRawTextElement
import org.jetbrains.compose.web.dom.isHtmlRcdataElement
import org.jetbrains.compose.web.dom.isHtmlVoidElement
import org.jetbrains.compose.web.dom.isValidRawTextContent
import org.jetbrains.compose.web.dom.normalizeHtmlInputCharacters
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable

internal sealed interface HtmlEncoding

// Stores encoded HTML as strings and child fragments, avoiding copies of nested HTML.
internal class StaticHtml(
    val tagName: String,
    private val parts: List<Any>,
    val length: Int,
    val height: Int,
) : HtmlEncoding {
    constructor(tagName: String, html: String) : this(tagName, listOf(html), html.length, 0)

    val html: String by lazy(LazyThreadSafetyMode.NONE) {
        parts.singleOrNull() as? String ?: buildString(length) { appendTo(this) }
    }

    fun appendTo(output: StringBuilder) {
        for (part in parts) {
            if (part is String) output.append(part) else (part as StaticHtml).appendTo(output)
        }
    }
}

// Checks which elements can become static HTML and which content can move out of its lambda.
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class HtmlAnalysis(
    private val symbols: HtmlSymbols,
    private val constants: ConstantEvaluator,
    private val elements: Map<IrCall, HtmlElement>,
) {
    private object Unsupported : HtmlEncoding

    // Hitting the depth limit here must still allow a later attempt starting at this element.
    private object TooDeep : HtmlEncoding

    private val cache = IdentityHashMap<IrCall, Node>()

    // Children retain original statements until the chosen plan is emitted.
    inner class Node(val element: HtmlElement) {
        val noContent = element.content.isAbsent()
        private val statements = if (noContent) emptyList() else
            ((element.content as? IrFunctionExpression)?.function?.body as? IrBlockBody)?.statements
        val children: List<HtmlChild>? by lazy(LazyThreadSafetyMode.NONE) { statements?.map(::classify) }
        val movableChildren: List<HtmlChild>? by lazy(LazyThreadSafetyMode.NONE) {
            if (canMoveContent()) children else null
        }
        val html: StaticHtml? get() = encode(0) as? StaticHtml
        private var encoded: HtmlEncoding? = null

        fun encode(depth: Int): HtmlEncoding {
            encoded?.let { return it }
            if (depth > MaxEncodingRecursion) return TooDeep
            var result = encodeUncached(this, depth)
            // At this depth, reaching the recursion limit proves the subtree exceeds MaxHtmlNestingDepth.
            // Cache the rejection, retrying at a smaller depth cannot make the subtree fit.
            if (result === TooDeep && MaxEncodingRecursion - depth > MaxHtmlNestingDepth) result = Unsupported
            if (result !== TooDeep) encoded = result
            return result
        }

        // Moving the body must preserve parser behavior and must not need the original lambda's
        // receiver or return target. A single dynamic Text call is left for prepared-element lowering.
        private fun canMoveContent(): Boolean {
            val tag = element.tag
            // Empty elements have no child parser rules to preserve. The mixed writer checks their parent.
            if (noContent && element.attributes is MixedAttributes && tag != "noscript") return true
            val void = isHtmlVoidElement(tag, element.namespace)
            // Literal regions enter their root, a void root would never leave it.
            if (!hasMovableContent(
                    tag,
                    element.namespace
                ) || (void && element.attributes is LiteralAttributes)
            ) return false
            if (noContent) return true
            val body = statements ?: return false
            val function = (element.content as? IrFunctionExpression)?.function ?: return false
            if (void && body.isNotEmpty()) return false
            val references = requireNotNull(element.content).contentScopeReferences()
            if (function.symbol in references || function.parameters.any { it.symbol in references }) return false
            // Preserve the specialized prepared element for a single composable text value.
            val text = body.singleOrNull() as? IrCall
            return text == null || text.symbol != symbols.text || symbols.isPlainText(text)
        }
    }

    // Analyze each original element once and share the result between lowering attempts.
    fun analyze(element: HtmlElement): Node = cache.getOrPut(element.call) { Node(element) }

    private fun classify(statement: IrStatement): HtmlChild {
        if (constants.isConstantAlias(statement)) return HtmlChild.Alias(statement)
        val call = statement as? IrCall ?: return HtmlChild.Runtime(statement)
        if (call.symbol == symbols.text) return HtmlChild.Text(call, constants)
        val element = elements[call] ?: return HtmlChild.Runtime(call)
        return HtmlChild.Element(analyze(element))
    }

    private fun encodeUncached(node: Node, depth: Int): HtmlEncoding {
        val element = node.element
        val tag = element.tag
        if (element.namespace == HtmlNamespace && tag == "noscript") return Unsupported
        val opening = element.openingTag ?: return Unsupported

        val fragment = if (isHtmlVoidElement(tag, element.namespace)) {
            // Void content still executes at runtime, even though its HTML is discarded.
            if (!node.noContent) return Unsupported
            StaticHtml(tag, opening)
        } else {
            val children = expandRepeats(node.children ?: return Unsupported) ?: return Unsupported
            if (isHtmlRawTextElement(tag, element.namespace)) {
                val html = encodeRawText(tag, opening, children) ?: return Unsupported
                StaticHtml(tag, html)
            } else {
                val result = encodeChildren(
                    tag,
                    element.namespace,
                    opening,
                    children,
                    isHtmlRcdataElement(tag, element.namespace),
                    depth
                )
                result as? StaticHtml ?: return result
            }
        }
        if (fragment.length > MaxEncodedFragmentLength) return Unsupported
        return fragment
    }

    private fun encodeRawText(tag: String, opening: String, children: List<HtmlChild>): String? {
        val text = StringBuilder()
        for (child in children) {
            if (child !is HtmlChild.Text) return null
            text.append(child.value ?: return null)
            if (text.length > MaxEncodedFragmentLength) return null
        }
        // Join before normalizing and validating so tags and CRLF split across Text calls are checked together.
        // Raw text replaces NUL with U+FFFD, matching runtime serialization and HTML parsing.
        val value = text.toString().normalizeHtmlInputCharacters()
        if (!isValidRawTextContent(tag, value)) return null
        return "$opening$value</$tag>"
    }

    // Escapes text and references encoded child elements.
    private fun encodeChildren(
        tag: String,
        namespace: String,
        opening: String,
        children: List<HtmlChild>,
        rcdata: Boolean,
        depth: Int,
    ): HtmlEncoding {
        val parts = mutableListOf<Any>()
        val pending = StringBuilder(opening)
        var length = 0
        var height = 0
        var firstContent = true
        var previousWasText = false
        for (child in children) {
            if (child is HtmlChild.Text) {
                val value = child.valueWithoutNul ?: return Unsupported
                if (namespace == HtmlNamespace && !isHtmlParserStableTableText(tag, value)) return Unsupported
                if (value.isNotEmpty()) {
                    if (previousWasText && !rcdata) pending.appendHydrationTextBoundaryMarker()
                    if (firstContent && isHtmlLeadingNewlineElement(tag, namespace) && value.first() == '\n') {
                        pending.append("\n")
                    }
                    pending.append(escapeHtml(value, attribute = false))
                    firstContent = false
                    previousWasText = true
                }
            } else {
                // RCDATA cannot contain parsed element children.
                if (rcdata) return Unsupported
                val element = child as? HtmlChild.Element ?: return Unsupported
                val result = element.node.encode(depth + 1)
                val fragment = result as? StaticHtml ?: return result
                if (fragment.height + 1 > MaxHtmlNestingDepth) return Unsupported
                if (namespace == HtmlNamespace && !isHtmlParserStableTableElement(
                        tag,
                        fragment.tagName
                    )
                ) return Unsupported
                if (pending.isNotEmpty()) {
                    parts.add(pending.toString())
                    length += pending.length
                    pending.setLength(0)
                }
                parts.add(fragment)
                length += fragment.length
                height = maxOf(height, fragment.height + 1)
                firstContent = false
                previousWasText = false
            }
            if (length.toLong() + pending.length > MaxEncodedFragmentLength) return Unsupported
        }
        pending.append("</$tag>")
        parts.add(pending.toString())
        length += pending.length
        return StaticHtml(tag, parts, length, height)
    }

    // Expand constant repeat calls only when the whole body can become static HTML.
    private fun expandRepeats(statements: List<HtmlChild>, depth: Int = 0): List<HtmlChild>? {
        if (depth > MaxHtmlNestingDepth) return null
        val calls = mutableListOf<HtmlChild>()
        for (statement in statements) {
            // The entire content lambda is replaced; its pure local aliases have no remaining uses.
            if (statement is HtmlChild.Alias) continue
            val call = statement.statement as? IrCall ?: return null
            if (call.symbol.owner.fqNameWhenAvailable == KotlinRepeat) {
                val count = constants.integer(call.arguments.getOrNull(0)) ?: return null
                if (count <= 0) continue
                if (count > MaxStaticRepeatCount) return null
                val content = call.arguments.getOrNull(1) as? IrFunctionExpression ?: return null
                val body = content.function.body as? IrBlockBody ?: return null
                val children = expandRepeats(body.statements.map(::classify), depth + 1) ?: return null
                if (children.isEmpty()) continue
                // Bound expansion independently of encoded output and literal chunk sizes.
                if (calls.size.toLong() + children.size.toLong() * count > MaxExpandedHtmlCalls) return null
                repeat(count) { calls.addAll(children) }
            } else {
                if (statement is HtmlChild.Runtime) return null
                calls.add(statement)
            }
            if (calls.size > MaxExpandedHtmlCalls) return null
        }
        return calls
    }
}

internal sealed class HtmlChild(val statement: IrStatement) {
    class Text(statement: IrCall, private val constants: ConstantEvaluator) : HtmlChild(statement) {
        private object Unevaluated

        // Cache failed folds too, without allocating a Lazy for each Text call.
        private var constant: Any? = Unevaluated
        val valueWithoutNul: String? get() = value?.withoutNul()
        val value: String?
            get() {
                if (constant === Unevaluated) constant =
                    constants.string((statement as IrCall).arguments.singleOrNull())
                return constant as? String
            }
    }

    class Element(val node: HtmlAnalysis.Node) : HtmlChild(node.element.call)
    class Alias(statement: IrStatement) : HtmlChild(statement)
    class Runtime(statement: IrStatement) : HtmlChild(statement)
}

// These tags allow content to move without changing how the parser or writer handles it.
private fun hasMovableContent(tag: String, namespace: String): Boolean =
    (namespace != HtmlNamespace || (tag != "noscript" && tag !in TableTags)) &&
            !isHtmlRawTextElement(tag, namespace) && !isHtmlRcdataElement(tag, namespace) &&
            !isHtmlLeadingNewlineElement(tag, namespace)

private val TableTags = setOf("table", "caption", "colgroup", "col", "thead", "tbody", "tfoot", "tr", "td", "th")
