/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.lowering

import org.jetbrains.compose.html.compiler.HtmlSymbols
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.reportInfo
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

// Lowers one file and keeps its analysis, generated fields and size limits together.
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class FileLowering(
    private val context: IrPluginContext,
    private val configuration: CompilerConfiguration,
    private val symbols: HtmlSymbols,
    private val file: IrFile,
) : IrElementTransformerVoidWithContext() {
    private val constants = ConstantEvaluator(context)
    private val attributeAnalyzer = HtmlAttributeAnalyzer(context, symbols, constants)
    private val wrappers = WrapperCallNormalizer(context, symbols, constants, attributeAnalyzer)
    private val elements = wrappers.elements
    private val fields = HtmlFields(context, file)
    private val analysis = HtmlAnalysis(symbols, constants, elements)
    private val attributeWriter = HtmlAttributeWriter(context, symbols, constants, fields)
    private val elementLowering = ElementLowering(context, symbols, analysis, attributeWriter, ::transform)
    private val prepared = PreparedElementLowering(context, symbols, fields)

    private var receiverCallsOnly = true

    // Normalize wrappers, lower elements, then add the fields needed by the generated code.
    fun lower() {
        file.transform(wrappers, null)
        // Lower calls with a content receiver first. Then enclosing content can move if it no longer
        // uses that receiver. Calls left at runtime keep their original scope.
        file.transform(this, null)
        receiverCallsOnly = false
        file.transform(this, null)

        file.declarations.addAll(0, prepared.fields)
        file.declarations.addAll(0, fields.stringFields)

        if (prepared.fields.isNotEmpty()) {
            configuration.reportInfo(
                "[compose-html-lowering] ${file.fileEntry.name}: " +
                        "prepared ${prepared.fields.size} HTML attribute value(s)"
            )
        }
    }

    // Try static HTML or a mix of HTML and runtime calls. Otherwise lower the children
    // and try caching the attributes for the original element call.
    override fun visitCall(expression: IrCall): IrExpression {
        if (receiverCallsOnly) {
            val visited = super.visitCall(expression) as IrCall
            if (expression.symbol.owner.parameters.none { it.kind == IrParameterKind.ExtensionReceiver }) return visited
            val element = elements[visited] ?: return visited
            return elementLowering.lower(element, builder(visited), owner()) ?: visited
        }
        val element = elements[expression]
        val lowered = element?.let { elementLowering.lower(it, builder(expression), owner()) }
        if (lowered != null) return lowered

        val visited = super.visitCall(expression)
        return element?.let { prepared.lower(it, builder(expression)) } ?: visited
    }

    private fun transform(statement: IrStatement): IrStatement = statement.transform(this, null) as IrStatement

    // Count moved content against the enclosing function's chunk limit.
    private fun owner() = requireNotNull(allScopes.firstOrNull { it.irElement is IrFunction } ?: currentScope)
        .scope.scopeOwnerSymbol

    private fun builder(expression: IrCall) = DeclarationIrBuilder(
        context,
        requireNotNull(currentScope).scope.scopeOwnerSymbol,
        expression.startOffset,
        expression.endOffset,
    )
}
