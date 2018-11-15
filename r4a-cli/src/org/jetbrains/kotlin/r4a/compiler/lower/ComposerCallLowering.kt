package org.jetbrains.kotlin.r4a.compiler.lower

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpressionWithCopy
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.r4a.ast.*
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxTag

fun lowerComposerFunction(context: GeneratorContext, container: IrPackageFragment, compose: IrFunction) {
    // Stores function global information
    val helper = ComposerLowerHelper(context, compose)

    context.symbolTable.withScope(compose.descriptor) {
        // Transform the KTX tags within compose
        compose.body!!.accept(object : IrElementTransformer<Nothing?> {
            override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?): IrElement {
                val block = IrBlockImpl(
                    expression.startOffset,
                    expression.endOffset,
                    context.irBuiltIns.unitType,
                    KTX_TAG_ORIGIN,
                    transform(context, container, compose.descriptor, expression as IrKtxTag, helper)
                )
                block.accept(this, data)
                return block
            }
        }, null)
    }
}

private fun transform(
    context: GeneratorContext,
    container: IrPackageFragment,
    owner: DeclarationDescriptor,
    tag: IrKtxTag,
    helper: ComposerLowerHelper
): List<IrStatement> {
    val tagIndex = helper.uniqueIndex()
    val output = mutableListOf<IrStatement>()
    val info = context.bindingContext.get(R4AWritableSlices.KTX_TAG_INFO, tag.element) ?: error("No tagInfo found on element")

    info.resolvedCalls?.let { resolvedCalls ->

        val attributeValues = tag.attributes.map { it.element to it.value }.toMap()
        val attributeGetters = resolvedCalls.usedAttributes.map { attrNode ->
            val expression = attrNode.expression
            val name = attrNode.name

            val attrVariable = IrTemporaryVariableDescriptorImpl(
                helper.compose.descriptor,
                Name.identifier("__el_attr_${tagIndex}_$name"),
                attrNode.type,
                false
            )

            val attributeValue =
                attributeValues[attrNode.expression] ?: error("Could not find attribute value for attribute ${attrNode.name}")

            val getValue = if (attributeValue is IrConst<*>) {
                attributeValue
            } else {
                val attrVariableDeclaration = context.symbolTable.declareVariable(
                    expression.startOffset, expression.endOffset,
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                    attrVariable,
                    attrNode.type.toIrType()!!,
                    attributeValue
                )

                // OUTPUT: val _el_attrName = (...attrExpression...)
                output.add(attrVariableDeclaration)

                IrGetValueImpl(
                    expression.startOffset, expression.endOffset,
                    context.symbolTable.referenceVariable(attrVariable)
                )
            }

            attrNode to getValue
        }.toMap()

        val emitOrCall = resolvedCalls.emitOrCall
        output.add(
            when (emitOrCall) {
                is NonMemoizedCallNode -> transformCallNode(emitOrCall, attributeGetters, helper)
                is MemoizedCallNode -> transformMemoizedCall(emitOrCall, attributeGetters, helper)
                is EmitCallNode -> transformEmit(emitOrCall, attributeGetters, helper)
                else -> error("Unexpeted call $emitOrCall")
            }
        )
    }
    return output
}

private fun transformCallNode(
    call: NonMemoizedCallNode,
    attributeGetters: Map<AttributeNode, IrExpressionWithCopy>,
    helper: ComposerLowerHelper): IrCall {
    return IrCallImpl(
        helper.startOffset,
        helper.endOffset,
        call.resolvedCall.resultingDescriptor.returnType!!.toIrType()!!,
        helper.context.symbolTable.referenceFunction(call.resolvedCall.resultingDescriptor)
    ).apply {
        call.params.forEachIndexed { index, param ->
            putValueArgument(index, attributeGetters[param] ?: error("Could not map argument for ${param.name}"))
        }
    }
}

private fun transformMemoizedCall(
    call: MemoizedCallNode,
    attributeGetters: Map<AttributeNode, IrExpressionWithCopy>,
    helper: ComposerLowerHelper): IrCall {
    error("Not implemented")
}

private fun transformEmit(
    emit: EmitCallNode,
    attributeGetters: Map<AttributeNode, IrExpressionWithCopy>,
    helper: ComposerLowerHelper): IrCall {
    error("Not implemented")
}



private class ComposerLowerHelper(val context: GeneratorContext, val compose: IrFunction) {
    private var myUniqueIndex = 0
    fun uniqueIndex() = myUniqueIndex++

    val startOffset = compose.startOffset
    val endOffset = compose.endOffset
}