package org.jetbrains.kotlin.r4a.compiler.lower

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.r4a.GeneratedKtxChildrenLambdaClassDescriptor
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxTag
import org.jetbrains.kotlin.r4a.compiler.ir.buildWithScope
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny

fun generateChildrenLambda(context: GeneratorContext, containingDeclaration: ClassDescriptor, capturedAccesses: List<IrValueAccessExpression>, bodyLambdaPsi: KtFunctionLiteral, body: Collection<IrStatement>): IrClass {

    val syntheticClassDescriptor = GeneratedKtxChildrenLambdaClassDescriptor(context.moduleDescriptor, containingDeclaration, capturedAccesses.map { it.type }, emptyList())
    val lambdaClass = context.symbolTable.declareClass(-1, -1, IrDeclarationOrigin.DEFINED, syntheticClassDescriptor)

    lambdaClass.createParameterDeclarations()
    syntheticClassDescriptor.capturedAccessesAsProperties.forEach { lambdaClass.declarations.add(context.symbolTable.declareField(it.startOffset ?: -1, it.endOffset ?: -1, IrDeclarationOrigin.DEFINED, it)) }
    lambdaClass.declarations.add(generateConstructor(context, syntheticClassDescriptor))
    lambdaClass.declarations.add(generateInvokeFunction(context, syntheticClassDescriptor, capturedAccesses, body))

    return lambdaClass
}

private fun generateConstructor(context: GeneratorContext, syntheticClassDescriptor: GeneratedKtxChildrenLambdaClassDescriptor): IrConstructor {
    return context.symbolTable.declareConstructor(
        -1,
        -1,
        IrDeclarationOrigin.DEFINED,
        syntheticClassDescriptor.unsubstitutedPrimaryConstructor
    )
        .buildWithScope(context) { constructor ->
            constructor.createParameterDeclarations()
            val lambdaAsThisReceiver = context.symbolTable.declareValueParameter(
                -1,
                -1,
                IrDeclarationOrigin.DEFINED,
                syntheticClassDescriptor.thisAsReceiverParameter
            ).symbol
            val getThisExpr = IrGetValueImpl(-1, -1, lambdaAsThisReceiver)

            val statements = mutableListOf<IrStatement>()
            val superConstructor =
                context.symbolTable.referenceConstructor(syntheticClassDescriptor.getSuperClassOrAny().constructors.single { it.valueParameters.size == 0 })
            val superCall = IrDelegatingConstructorCallImpl(-1, -1, superConstructor, superConstructor.descriptor, 0)

            statements.add(superCall)

            statements.add(IrInstanceInitializerCallImpl(-1, -1, context.symbolTable.referenceClass(syntheticClassDescriptor)))

            constructor.valueParameters.forEachIndexed { index, irValueParameter ->
                val fieldSymbol = context.symbolTable.referenceField(syntheticClassDescriptor.capturedAccessesAsProperties[index])
                statements.add(IrSetFieldImpl(-1, -1, fieldSymbol, getThisExpr, IrGetValueImpl(-1, -1, irValueParameter.symbol)))
            }

            constructor.body = IrBlockBodyImpl(-1, -1, statements)
        }
}

private fun generateInvokeFunction(context: GeneratorContext, syntheticClassDescriptor: GeneratedKtxChildrenLambdaClassDescriptor, capturedAccesses: List<IrValueAccessExpression>, body: Collection<IrStatement>): IrFunction {
    val functionDescriptor = syntheticClassDescriptor.invokeDescriptor

    val lambdaAsThisReceiver = context.symbolTable.declareValueParameter(
        -1,
        -1,
        IrDeclarationOrigin.DEFINED,
        syntheticClassDescriptor.thisAsReceiverParameter
    ).symbol

    val transformedBody = body.map {
        it.transform(object : IrElementTransformer<Nothing?> {
            override fun visitValueAccess(expression: IrValueAccessExpression, data: Nothing?): IrExpression {
                if(expression in capturedAccesses && expression is IrGetValue) {
                    val newSymbol = context.symbolTable.referenceField(syntheticClassDescriptor.capturedAccessesAsProperties[capturedAccesses.indexOf(expression)])
                    return IrGetFieldImpl(expression.startOffset, expression.endOffset, newSymbol, IrGetValueImpl(-1, -1, lambdaAsThisReceiver), expression.origin)
                }
                else return expression
            }
            override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?): IrElement {
                expression.transformChildren(this, data)
                return expression
            }
        }, null)
    }


    return context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
        .buildWithScope(context) { irFunction ->
            irFunction.createParameterDeclarations()

            irFunction.body = IrBlockBodyImpl(-1, -1, transformedBody.toList())
        }
}
