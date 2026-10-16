/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.compose.web.dom.HtmlNamespace
import org.jetbrains.compose.web.dom.isHtmlLeadingNewlineElement
import org.jetbrains.compose.web.dom.isHtmlParserStableTableElement
import org.jetbrains.compose.web.dom.isHtmlParserStableTableText
import org.jetbrains.compose.web.dom.isHtmlRawTextElement
import org.jetbrains.compose.web.dom.isHtmlRcdataElement
import org.jetbrains.compose.web.dom.isHtmlVoidElement
import org.jetbrains.compose.web.dom.isValidRawTextContent
import org.jetbrains.compose.web.dom.isValidHtmlTagName
import org.jetbrains.compose.web.dom.normalizeHtmlInputCharacters
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.Name

// Keep each generated string safely below the JVM constant-pool limit.
private const val MaxEncodedLength = 8_000
private const val MaxDepth = 64

internal class EncodedHtmlFragment(val tagName: String, val html: String)

/** Produces HTML string for complete, constant trees. A null result keeps the original call. */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class StaticHtmlTree(private val symbols: HtmlSymbols) {
    private val attributes = StaticHtmlAttributes(symbols)

    // Checks if called function has a `@LowerableHtmlTag` annotation. Resolve then its parameters.
    private fun tagName(call: IrCall): String? {
        val function = call.symbol.owner
        val annotation = function.annotations.singleOrNull { it.classSymbol == symbols.tagMarkerSymbol }
            ?: return null
        val explicitName = (annotation.argumentMapping[Name.identifier("tagName")] as? IrConst)?.value as? String
        return explicitName?.takeIf { it.isNotEmpty() } ?: function.name.asString().lowercase()
    }

    // Encodes call, if possible, as HTML fragment. Recognizes tag, encodes attributes & inspects content.
    fun encode(call: IrCall, depth: Int = 0): EncodedHtmlFragment? {
        val tag = tagName(call) ?: return null
        if (!isValidHtmlTagName(tag) || tag == "noscript") return null
        if (depth > MaxDepth || call.arguments.size !in 1..2) return null
        val attrs = attributes.encode(call.arguments[0], tag) ?: return null
        val content = call.arguments.getOrNull(1)
        val noContent = content == null || (content as? IrConst)?.kind == IrConstKind.Null

        val html = if (isHtmlVoidElement(tag, HtmlNamespace)) {
            // Void content still executes at runtime, even though its HTML is discarded.
            if (!noContent) return null
            "<$tag$attrs>"
        } else {
            val statements = when {
                noContent -> emptyList()
                content is IrFunctionExpression ->
                    (content.function.body as? IrBlockBody)?.statements ?: return null
                else -> return null
            }
            if (isHtmlRawTextElement(tag, HtmlNamespace)) {
                encodeRawText(tag, attrs, statements)
            } else {
                encodeChildren(tag, attrs, statements, isHtmlRcdataElement(tag, HtmlNamespace), depth)
            } ?: return null
        }
        return EncodedHtmlFragment(tag, html).takeIf { html.length <= MaxEncodedLength }
    }

    private fun encodeRawText(tag: String, attrs: String, statements: List<IrStatement>): String? {
        val text = StringBuilder()
        for (statement in statements) {
            val child = statement as? IrCall ?: return null
            if (child.symbol != symbols.textSymbol) return null
            text.append(literalText(child) ?: return null)
            if (text.length > MaxEncodedLength) return null
        }
        // Join before normalizing and validating so tags and CRLF split across Text calls are checked together.
        val value = text.toString().normalizeHtmlInputCharacters()
        if (!isValidRawTextContent(tag, value)) return null
        return "<$tag$attrs>$value</$tag>"
    }

    // Escapes text and recursively encodes child elements.
    private fun encodeChildren(
        tag: String,
        attrs: String,
        statements: List<IrStatement>,
        rcdata: Boolean,
        depth: Int,
    ): String? {
        val html = StringBuilder("<$tag$attrs>")
        var firstContent = true
        var previousWasText = false
        for (statement in statements) {
            val child = statement as? IrCall ?: return null
            if (child.symbol == symbols.textSymbol) {
                val value = literalText(child) ?: return null
                if ('\u0000' in value || !isHtmlParserStableTableText(tag, value)) return null
                if (value.isNotEmpty()) {
                    // Separate text nodes need hydration boundary markers.
                    if (previousWasText && !rcdata) return null
                    if (firstContent && isHtmlLeadingNewlineElement(tag, HtmlNamespace) && value.first() == '\n') {
                        html.append('\n')
                    }
                    html.append(escapeHtml(value, attribute = false))
                    firstContent = false
                    previousWasText = true
                }
            } else {
                // RCDATA cannot contain parsed element children.
                if (rcdata) return null
                val fragment = encode(child, depth + 1) ?: return null
                if (!isHtmlParserStableTableElement(tag, fragment.tagName)) return null
                html.append(fragment.html)
                firstContent = false
                previousWasText = false
            }
            if (html.length > MaxEncodedLength) return null
        }
        html.append("</$tag>")
        return html.toString()
    }

    private fun literalText(call: IrCall): String? =
        (call.arguments.singleOrNull() as? IrConst)?.value as? String
}
