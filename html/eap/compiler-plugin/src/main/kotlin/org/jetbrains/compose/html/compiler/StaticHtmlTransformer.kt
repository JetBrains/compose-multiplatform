/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.reportInfo
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irAnnotation
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.Name

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class StaticHtmlTransformer(
    private val context: IrPluginContext,
    private val configuration: CompilerConfiguration,
    private val symbols: HtmlSymbols,
    private val stringRenderingFunctions: Set<IrFunction>,
) : IrElementTransformerVoidWithContext() {
    private val tree = StaticHtmlTree(symbols)
    private val pendingFields = mutableListOf<IrField>()
    private val usedNames = mutableSetOf<Name>()

    // Prepare fields, traverse the file, then insert static fragments at the beginning.
    override fun visitFileNew(declaration: IrFile): IrFile {
        pendingFields.clear()
        usedNames.clear()
        usedNames.addAll(declaration.declarations.filterIsInstance<IrDeclarationWithName>().map { it.name })
        super.visitFileNew(declaration)
        // Traverse first so generated fallback lambdas are not lowered again.
        // Initialize fragments before authored fields that might render during initialization.
        declaration.declarations.addAll(0, pendingFields)
        return declaration
    }

    // Try to encode entire subtree in a precomputed fragment.
    override fun visitCall(expression: IrCall): IrExpression {
        // Only lower content reached from composeHtmlToString.
        if (currentFunction?.irElement !in stringRenderingFunctions) return super.visitCall(expression)

        // Try to walk whole element recursively, otherwise visit children via visitor (argument of expression)
        val fragment = tree.encode(expression) ?: return super.visitCall(expression)
        val field = createFragment(expression, fragment)
        pendingFields.add(field)

        val line = currentFile.fileEntry.getLineNumber(expression.startOffset) + 1
        configuration.reportInfo(
            "[compose-html-lowering] ${currentFile.fileEntry.name}:$line: " +
                "precomputed <${fragment.tagName}> subtree (${fragment.html.length} characters)"
        )

        val builder = DeclarationIrBuilder(
            context,
            requireNotNull(currentScope).scope.scopeOwnerSymbol,
            expression.startOffset,
            expression.endOffset
        )
        return builder.irCall(symbols.renderFragmentSymbol).apply {
            arguments[0] = builder.irGetField(null, field)
        }
    }

    /**
     * Creates IR for generated field. Example:
     * ```
     * private val __htmlFragment0 = PrecomputedHtmlFragment(
     *    "div",
     *    "<div>Hello</div>",                  // static snippet for SSR
     *    content = { Div { Text("Hello") } } // fallback for client rendering
     * )
     * ```
     */
    private fun createFragment(original: IrCall, fragment: EncodedHtmlFragment): IrField {
        val field = context.irFactory.buildField {
            name = nextFieldName()
            type = symbols.fragmentConstructorSymbol.owner.returnType
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
            isStatic = true
            startOffset = original.startOffset
            endOffset = original.endOffset
        }.apply { parent = currentFile }

        val builder = DeclarationIrBuilder(context, field.symbol, original.startOffset, original.endOffset)
        // Fallback as lambda.
        val content = context.irFactory.buildFun {
            name = Name.special("<anonymous>")
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            visibility = DescriptorVisibilities.LOCAL
            returnType = context.irBuiltIns.unitType
            startOffset = original.startOffset
            endOffset = original.endOffset
        }.apply {
            parent = currentFile
            annotations = listOf(builder.irAnnotation(symbols.composableConstructorSymbol))
            body = context.irFactory.createBlockBody(startOffset, endOffset, listOf(original))
        }

        val initialize = builder.irCallConstructor(symbols.fragmentConstructorSymbol, emptyList()).apply {
            arguments[0] = builder.irString(fragment.tagName)
            arguments[1] = builder.irString(fragment.html)
            arguments[2] = IrFunctionExpressionImpl(
                original.startOffset,
                original.endOffset,
                symbols.fragmentConstructorSymbol.owner.parameters[2].type,
                content,
                IrStatementOrigin.LAMBDA
            )
        }
        field.initializer = context.irFactory.createExpressionBody(initialize)
        field.patchDeclarationParents(currentFile)
        return field
    }

    // Returns a unique name within the file, e.g. __htmlFragment0, __htmlFragment1, ...
    private fun nextFieldName(): Name {
        var index = pendingFields.size
        while (true) {
            val name = Name.identifier("__htmlFragment${index++}")
            if (usedNames.add(name)) return name
        }
    }
}
