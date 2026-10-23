/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.lowering

import org.jetbrains.compose.html.compiler.HtmlSymbols
import org.jetbrains.compose.html.compiler.ir.KotlinRepeat
import org.jetbrains.compose.html.compiler.ir.hasComposableCall
import org.jetbrains.compose.html.compiler.ir.irCallWithArgs
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName

/**
 * Reuses writer and validation lookups across nested writes and supported loops.
 * Replaces `Text` calls when their value does not call composable code.
 *
 * ```kotlin
 * for (name in names) {
 *     Text(name)
 * }
 * ```
 *
 * Becomes, in simplified Kotlin:
 * ```kotlin
 * val writer = currentHtmlStringWriter()
 * val validate = currentHtmlValidationIsStrict()
 * for (name in names) {
 *     writeText(writer, name, validate)
 * }
 * ```
 *
 * Ordinary functions and callbacks keep their own scope. Supported eager loop lambdas
 * such as `repeat` and `forEach` can share the enclosing scope.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class WriterScopeSharing(
    private val context: IrPluginContext,
    private val symbols: HtmlSymbols,
) : IrElementTransformerVoidWithContext() {
    private var writerScope: WriterScopeOrigin? = null
    private val aliases = mutableMapOf<IrValueSymbol, IrVariable>()
    private val loopFunctions = mutableSetOf<IrFunction>()

    override fun visitFunctionNew(declaration: IrFunction): IrFunction {
        val previous = writerScope
        if (declaration !in loopFunctions) writerScope = null
        return try {
            super.visitFunctionNew(declaration) as IrFunction
        } finally {
            writerScope = previous
        }
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        // Compose expects the FOR_LOOP block to end in its original while loop.
        if (expression.origin == IrStatementOrigin.FOR_LOOP) {
            return withLoopScope(expression) { super.visitBlock(expression) }
        }
        val scope = expression.origin as? WriterScopeOrigin ?: return super.visitBlock(expression)
        val writer = scope.writer
        val validate = scope.validate
        val previous = writerScope
        if (previous == null) {
            writerScope = scope
        } else {
            aliases[writer.symbol] = previous.writer
            aliases[validate.symbol] = previous.validate
            expression.statements.removeAll { it === writer || it === validate }
        }
        return try {
            super.visitBlock(expression)
        } finally {
            writerScope = previous
            aliases.remove(writer.symbol)
            aliases.remove(validate.symbol)
        }
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression =
        aliases[expression.symbol]?.let { builder(expression).irGet(it) } ?: super.visitGetValue(expression)

    override fun visitLoop(loop: IrLoop): IrExpression = withLoopScope(loop) { super.visitLoop(loop) }

    override fun visitCall(expression: IrCall): IrExpression {
        val scope = writerScope
        if (scope != null && symbols.isPlainText(expression)) {
            val visited = super.visitCall(expression) as IrCall
            val builder = builder(expression)
            return builder.irCallWithArgs(symbols.writeText,
                builder.irGet(scope.writer), visited.arguments.single(), builder.irGet(scope.validate)
            )
        }
        if (!isEagerLoop(expression)) return super.visitCall(expression)
        val functions = expression.arguments.filterIsInstance<IrFunctionExpression>().map { it.function }
        loopFunctions.addAll(functions)
        return try {
            withLoopScope(expression) { super.visitCall(expression) }
        } finally {
            loopFunctions.removeAll(functions.toSet())
        }
    }

    // Reuse an outer scope, or create one outside a loop that contains direct writes.
    private inline fun withLoopScope(expression: IrExpression, visit: () -> IrExpression): IrExpression {
        if (writerScope != null || !containsDirectWrites(expression)) return visit()
        val builder = builder(expression)
        return builder.irWriterScope(symbols, expression.type) { writer, validate ->
            writerScope = WriterScopeOrigin(writer, validate)
            try {
                +visit()
            } finally {
                writerScope = null
            }
        }
    }

    // Look inside supported eager loops, skipping other function and callback bodies.
    private fun containsDirectWrites(expression: IrExpression): Boolean {
        var found = false
        expression.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (!found) element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                if (declaration in loopFunctions) super.visitFunction(declaration)
            }

            override fun visitCall(expression: IrCall) {
                if (expression.symbol == symbols.openLoweredRegion || expression.symbol == symbols.beginMixedElement ||
                    symbols.isPlainText(expression)) found = true
                if (isEagerLoop(expression)) {
                    // Descend through nested eager loops, but never stored or arbitrary callbacks.
                    expression.arguments.filterIsInstance<IrFunctionExpression>().forEach {
                        it.function.body?.acceptVoid(this)
                    }
                } else {
                    super.visitCall(expression)
                }
            }
        })
        return found
    }

    private fun builder(expression: IrExpression) = DeclarationIrBuilder(
        context, requireNotNull(currentScope).scope.scopeOwnerSymbol,
        expression.startOffset, expression.endOffset,
    )

    private fun isEagerLoop(call: IrCall): Boolean {
        val function = call.symbol.owner
        if (!function.isInline) return false
        return function.fqNameWhenAvailable in EagerLoops
    }

    private companion object {
        val EagerLoops = setOf(
            KotlinRepeat,
            FqName("kotlin.collections.forEach"),
            FqName("kotlin.collections.forEachIndexed"),
            FqName("kotlin.sequences.forEach"),
            FqName("kotlin.sequences.forEachIndexed"),
            FqName("kotlin.text.forEach"),
            FqName("kotlin.text.forEachIndexed"),
        )
    }
}

// Marks writer and validation locals so the sharing pass can reuse an outer pair.
internal class WriterScopeOrigin(val writer: IrVariable, val validate: IrVariable) : IrStatementOrigin {
    override val debugName = "HTML_WRITER_SCOPE"
    override fun toString(): String = debugName
}

// Read the writer and validation setting once and mark the block for the sharing pass.
internal inline fun DeclarationIrBuilder.irWriterScope(
    symbols: HtmlSymbols,
    resultType: IrType,
    body: IrBlockBuilder.(writer: IrVariable, validate: IrVariable) -> Unit,
): IrExpression {
    lateinit var scope: WriterScopeOrigin
    val block = irBlock(resultType = resultType) {
        val writer = irTemporary(irCall(symbols.currentHtmlStringWriter), "htmlWriter")
        val validate = irTemporary(irCall(symbols.currentHtmlValidationIsStrict), "htmlValidate")
        scope = WriterScopeOrigin(writer, validate)
        body(writer, validate)
    }
    block.origin = scope
    return block
}

// Sharing a writer is safe only when evaluating the text does not call composable code.
internal fun HtmlSymbols.isPlainText(call: IrCall): Boolean =
    call.symbol == text && !hasComposableCall(call.arguments.singleOrNull())
