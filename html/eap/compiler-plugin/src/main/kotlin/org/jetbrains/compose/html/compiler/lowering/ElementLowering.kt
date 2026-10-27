/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.html.compiler.lowering

import org.jetbrains.compose.html.compiler.HtmlSymbols
import org.jetbrains.compose.html.compiler.ir.irCallWithArgs
import org.jetbrains.compose.web.dom.escapeHtml
import org.jetbrains.compose.web.dom.isHtmlVoidElement
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

// Replaces elements with HTML strings, keeping runtime calls where needed.
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class ElementLowering(
    private val context: IrPluginContext,
    private val symbols: HtmlSymbols,
    private val analysis: HtmlAnalysis,
    private val attributeWriter: HtmlAttributeWriter,
    private val transform: (IrStatement) -> IrStatement,
) {
    private val functionChunks = mutableMapOf<IrSymbol, Int>()
    private var fileChunks = 0

    private fun remaining(owner: IrSymbol): Int =
        minOf(MaxGeneratedChunksPerFunction - (functionChunks[owner] ?: 0), MaxGeneratedChunksPerFile - fileChunks)

    private fun consume(owner: IrSymbol, chunks: Int) {
        check(chunks <= remaining(owner))
        functionChunks.merge(owner, chunks, Int::plus)
        fileChunks += chunks
    }

    private sealed interface Operation

    // An HTML chunk and the parent tag and namespace to use for the following runtime calls.
    private class Write(val html: String, val parent: String, val namespace: String) : Operation
    private class Statement(val ir: IrStatement) : Operation

    private class RegionPlan(
        val root: String,
        val namespace: String,
        val attributes: HtmlAttributes,
        private val budget: Int
    ) {
        val operations = mutableListOf<Operation>()
        val aliases = mutableListOf<IrStatement>()
        private val pending = StringBuilder()
        private var parent = root
        private var parentNamespace = namespace
        private var flushedLength = 0L
        var chunks = 0
            private set
        val endsWithTag: Boolean get() = pending.lastOrNull() == '>'
        private var closingLength = 0L
        private var closingChunks = 0

        // Leave room for every closing tag before adding an opening tag.
        // Runtime calls can split the HTML into separate chunks.
        private fun fits(addition: Int, closing: String = ""): Boolean =
            flushedLength + pending.length + addition + closingLength + closing.length <= MaxEncodedFragmentLength &&
                    chunks + chunkBound(pending.length + addition) + closingChunks + chunkBound(closing.length) <= budget

        // Add the opening, body and closing together, then restore the parent.
        inline fun element(
            tag: String,
            namespace: String,
            opening: String,
            prefix: String = "",
            body: () -> Unit
        ): Boolean {
            val closing = if (isHtmlVoidElement(tag, namespace)) "" else "</$tag>"
            if (!fits(prefix.length + opening.length, closing)) return false
            closingLength += closing.length
            closingChunks += chunkBound(closing.length)
            val previous = parent
            val previousNamespace = parentNamespace
            parent = tag
            parentNamespace = namespace
            pending.append(prefix)
            pending.append(opening)
            body()
            parent = previous
            parentNamespace = previousNamespace
            closingLength -= closing.length
            closingChunks -= chunkBound(closing.length)
            pending.append(closing)
            return true
        }

        fun tryAppend(html: String): Boolean = fits(html.length).also { if (it) pending.append(html) }

        fun tryAppend(html: StaticHtml, prefix: String = ""): Boolean {
            if (!fits(prefix.length + html.length)) return false
            pending.append(prefix)
            html.appendTo(pending)
            return true
        }

        fun flush() {
            if (pending.isEmpty()) return
            val literals = htmlChunks(pending.toString())
            chunks += literals.size
            flushedLength += pending.length
            for (literal in literals) operations.add(Write(literal, parent, parentNamespace))
            pending.setLength(0)
        }

        fun statement(statement: IrStatement) {
            flush()
            operations.add(Statement(statement))
        }
    }

    /**
     * Replaces an element with static HTML or HTML mixed with runtime calls.
     *
     * ```kotlin
     * Div { Text("Hello") } // Becomes a single HTML write.
     * Div { Text(name) }    // Keeps the dynamic text at runtime.
     * ```
     *
     * Returns `null` if the element cannot be lowered within the size limits.
     */
    fun lower(
        element: HtmlElement,
        builder: DeclarationIrBuilder,
        owner: IrSymbol,
    ): IrExpression? {
        val node = analysis.analyze(element)
        node.html?.let { lowerStatic(it, builder, owner) }?.let { return it }
        return lowerRegion(element, node.movableChildren ?: return null, builder, owner)
    }

    /**
     * Groups static HTML around calls that still run at runtime.
     *
     * ```kotlin
     * Div {
     *     Span { Text("Hello") }
     *     Text(name)
     * } => RegionPlan:
     * Write("<div><span>Hello</span>")
     * Statement(Text(name))
     * Write("</div>")
     * ```
     */
    private fun lowerRegion(
        element: HtmlElement,
        children: List<HtmlChild>,
        builder: DeclarationIrBuilder,
        owner: IrSymbol,
    ): IrExpression? {
        val attributes = element.attributes
        if (attributes is MixedAttributes && !attributeWriter.canEmit(attributes)) return null

        val plan = RegionPlan(element.tag, element.namespace, attributes, remaining(owner))
        val planned = plan.element(element.tag, element.namespace, element.openingTag.orEmpty()) {
            add(plan, children, depth = 0)
            // Keep opening and closing separate when no runtime statement flushed the opening:
            // otherwise openLoweredRegion and closeElement would write the same chunk.
            if (attributes is LiteralAttributes && plan.operations.isEmpty()) plan.flush()
        }

        if (!planned) return null
        plan.flush()
        // Consume before emitting so nested lowerings see the reduced budget.
        consume(owner, plan.chunks)

        return emit(plan, builder).also {
            it.patchDeclarationParents(builder.scope.scopeOwnerSymbol.owner as IrDeclarationParent)
        }
    }

    private fun add(plan: RegionPlan, children: List<HtmlChild>, depth: Int) {
        var index = 0
        while (index < children.size) {
            val child = children[index++]
            val absorbed = when (child) {
                is HtmlChild.Alias -> {
                    plan.aliases.add(child.statement)
                    true
                }

                is HtmlChild.Element -> absorb(plan, child.node, depth)
                is HtmlChild.Text -> {
                    // Pure aliases are hoisted even when text or the following element stays at runtime.
                    while (children.getOrNull(index) is HtmlChild.Alias) {
                        plan.aliases.add(children[index++].statement)
                    }
                    absorbText(plan, child, children.getOrNull(index), depth)
                        .also { if (it && index < children.size) index++ }
                }

                is HtmlChild.Runtime -> false
            }
            if (!absorbed) plan.statement(child.statement)
        }
    }

    private fun absorbText(plan: RegionPlan, text: HtmlChild.Text, next: HtmlChild?, depth: Int): Boolean {
        if (!plan.endsWithTag || (next != null && next !is HtmlChild.Element)) return false
        val value = text.valueWithoutNul?.takeIf { it.isNotEmpty() } ?: return false
        // Admit text together with its following element or the enclosing closing tag.
        val html = escapeHtml(value, attribute = false)
        return if (next is HtmlChild.Element) absorb(plan, next.node, depth, html) else plan.tryAppend(html)
    }

    private fun absorb(plan: RegionPlan, node: HtmlAnalysis.Node, depth: Int, prefix: String = ""): Boolean {
        val html = node.html
        if (html != null && plan.tryAppend(html, prefix)) return true
        if (depth >= MaxHtmlNestingDepth) return false
        val element = node.element
        val opening = element.openingTag ?: return false
        val children = node.movableChildren ?: return false
        return plan.element(element.tag, element.namespace, opening, prefix) { add(plan, children, depth + 1) }
    }

    // Writes the planned HTML and runtime statements, updating the writer's parent as it goes.
    private fun emit(plan: RegionPlan, builder: DeclarationIrBuilder) =
        builder.irWriterScope(symbols, context.irBuiltIns.unitType) { writer, validate ->
            for (alias in plan.aliases) +transform(alias)
            val root = plan.root
            val operations = ArrayDeque(plan.operations)
            val opening = when (val attributes = plan.attributes) {
                is LiteralAttributes -> {
                    val first = operations.removeFirst() as Write
                    irCallWithArgs(
                        symbols.openLoweredRegion,
                        irGet(writer), irString(root), irString(first.html), irString(first.parent),
                        irGet(validate), irString(first.namespace)
                    )
                }

                is MixedAttributes -> irIfThenElse(
                    symbols.finishMixedElement.owner.returnType,
                    irCallWithArgs(symbols.beginMixedElement, irGet(writer), irString(root), irGet(validate)),
                    transform(attributeWriter.emit(attributes, builder, writer, validate)) as IrExpression,
                    irNull()
                )
            }
            val closing = if (isHtmlVoidElement(root, plan.namespace)) null else operations.removeLast() as Write
            val parent = irTemporary(opening, "htmlParent")
            for (operation in operations) {
                +when (operation) {
                    is Statement -> transform(operation.ir)
                    is Write -> irCallWithArgs(
                        symbols.advanceLoweredRegion,
                        irGet(writer),
                        irString(operation.html),
                        irString(operation.parent),
                        irString(operation.namespace)
                    )
                }
            }
            if (closing != null) +irCallWithArgs(
                symbols.closeElement,
                irGet(writer), irString(closing.html), irGet(parent)
            )
        }

    // Writes a complete HTML fragment, splitting large strings into chunks.
    // Returns null if the chunks would exceed the function or file limit.
    private fun lowerStatic(html: StaticHtml, builder: DeclarationIrBuilder, owner: IrSymbol): IrExpression? {
        if (chunkBound(html.length) > remaining(owner)) return null
        val chunks = htmlChunks(html.html)
        consume(owner, chunks.size)
        if (chunks.size == 1) return builder.irCallWithArgs(
            symbols.writeStaticHtml, builder.irString(html.tagName), builder.irString(chunks.single())
        )

        return builder.irWriterScope(symbols, context.irBuiltIns.unitType) { writer, validate ->
            val begin = irCallWithArgs(
                symbols.beginStaticHtmlFragment,
                irGet(writer), irString(html.tagName), irGet(validate)
            )
            +irIfThen(context.irBuiltIns.unitType, begin, irBlock(resultType = context.irBuiltIns.unitType) {
                for (chunk in chunks) +irCallWithArgs(symbols.appendStaticHtmlChunk, irGet(writer), irString(chunk))
            })
        }
    }
}

// Upper bound on chunk count. Splitting a surrogate pair can move one code unit to the next chunk.
private fun chunkBound(length: Int): Int = when {
    length == 0 -> 0
    length <= MaxHtmlChunkLength -> 1
    else -> (length + MaxHtmlChunkLength - 2) / (MaxHtmlChunkLength - 1)
}

// Splits HTML into literals small enough for the JVM, keeping surrogate pairs together.
private fun htmlChunks(html: String): List<String> {
    if (html.length <= MaxHtmlChunkLength) return listOf(html)
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < html.length) {
        var end = minOf(start + MaxHtmlChunkLength, html.length)
        if (end < html.length && html[end - 1].isHighSurrogate() && html[end].isLowSurrogate()) end--
        chunks.add(html.substring(start, end))
        start = end
    }
    return chunks
}
