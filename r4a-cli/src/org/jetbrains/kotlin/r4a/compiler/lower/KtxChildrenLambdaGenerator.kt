package org.jetbrains.kotlin.r4a.compiler.lower

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.r4a.GeneratedKtxChildrenLambdaClassDescriptor
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxTag
import org.jetbrains.kotlin.r4a.compiler.ir.buildWithScope
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny

fun generateChildrenLambda(context: GeneratorContext, containingDeclaration: ClassDescriptor, body: Collection<IrStatement>): IrClass {

    val syntheticClassDescriptor = GeneratedKtxChildrenLambdaClassDescriptor(context.moduleDescriptor, containingDeclaration)
    val wrapperViewIrClass = context.symbolTable.declareClass(-1, -1, IrDeclarationOrigin.DEFINED, syntheticClassDescriptor)

    wrapperViewIrClass.createParameterDeclarations()
    wrapperViewIrClass.declarations.add(generateConstructor(context, syntheticClassDescriptor))
    wrapperViewIrClass.declarations.add(generateInvokeFunction(context, syntheticClassDescriptor, body))

    return wrapperViewIrClass
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
            val wrapperViewAsThisReceiver = context.symbolTable.declareValueParameter(
                -1,
                -1,
                IrDeclarationOrigin.DEFINED,
                syntheticClassDescriptor.thisAsReceiverParameter
            ).symbol
            val getThisExpr = IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver)

            val statements = mutableListOf<IrStatement>()
            val superConstructor =
                context.symbolTable.referenceConstructor(syntheticClassDescriptor.getSuperClassOrAny().constructors.single { it.valueParameters.size == 0 })
            val superCall = IrDelegatingConstructorCallImpl(-1, -1, superConstructor, superConstructor.descriptor, 0)

            statements.add(superCall)

            statements.add(IrInstanceInitializerCallImpl(-1, -1, context.symbolTable.referenceClass(syntheticClassDescriptor)))

            constructor.body = IrBlockBodyImpl(-1, -1, statements)
        }
}

private fun generateInvokeFunction(context: GeneratorContext, syntheticClassDescriptor: GeneratedKtxChildrenLambdaClassDescriptor, body: Collection<IrStatement>): IrFunction {
    val functionDescriptor = syntheticClassDescriptor.invokeDescriptor
    return context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
        .buildWithScope(context) { irFunction ->
            irFunction.createParameterDeclarations()

            irFunction.body = IrBlockBodyImpl(-1, -1, body.toList())
        }
}
