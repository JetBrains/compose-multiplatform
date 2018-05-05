package org.jetbrains.kotlin.r4a.compiler.lower

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.compiler.ir.buildWithScope
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny

fun generateRerenderHelper(context: GeneratorContext, componentMetadata: ComponentMetadata): IrClass {
    val renderHelperIrClass = context.symbolTable.declareClass(-1, -1, IrDeclarationOrigin.DEFINED, componentMetadata.renderHelperClassDescriptor)
    renderHelperIrClass.createParameterDeclarations()

    renderHelperIrClass.declarations.add(generateComponentInstanceField(context, componentMetadata))
    renderHelperIrClass.declarations.add(context.symbolTable.declareField(-1, -1, IrDeclarationOrigin.DEFINED, componentMetadata.renderHelperClassDescriptor.viewPropertyDescriptor))
    renderHelperIrClass.declarations.add(generateRunMethod(context, componentMetadata, renderHelperIrClass))
    renderHelperIrClass.declarations.add(generateConstructor(context, componentMetadata, renderHelperIrClass))

    return renderHelperIrClass
}

private fun generateConstructor(context: GeneratorContext, componentMetadata: ComponentMetadata, renderHelperIrClass: IrClass): IrConstructor {
    val helperConstructor = componentMetadata.renderHelperClassDescriptor.unsubstitutedPrimaryConstructor!!
    val viewFieldDescriptor = componentMetadata.renderHelperClassDescriptor.viewPropertyDescriptor
    val irConstructor = context.symbolTable.declareConstructor(-1, -1, IrDeclarationOrigin.DEFINED, helperConstructor)
        .buildWithScope(context) { irConstructor ->

            irConstructor.createParameterDeclarations()

            val superConstructor = context.symbolTable.referenceConstructor(componentMetadata.renderHelperClassDescriptor.getSuperClassOrAny().constructors.single { it.valueParameters.size == 0})
            val superCall = IrDelegatingConstructorCallImpl(-1, -1, superConstructor, superConstructor.descriptor, 0)

            val setField = IrSetFieldImpl(-1, -1, context.symbolTable.referenceField(viewFieldDescriptor), IrGetValueImpl(-1, -1, renderHelperIrClass.thisReceiver!!.symbol), IrGetValueImpl(-1, -1, irConstructor.valueParameters.first().symbol))

            irConstructor.body = IrBlockBodyImpl(-1, -1, listOf(superCall, setField))
        }
    return irConstructor
}

private fun generateComponentInstanceField(context: GeneratorContext, componentMetadata: ComponentMetadata) = IrFieldImpl(-1, -1, IrDeclarationOrigin.DEFINED, componentMetadata.renderHelperClassDescriptor.componentInstancePropertyDescriptor)

private fun generateRunMethod(context: GeneratorContext, componentMetadata: ComponentMetadata, renderHelperIrClass: IrClass): IrFunction {
    val viewFieldDescriptor = componentMetadata.renderHelperClassDescriptor.viewPropertyDescriptor
    val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, componentMetadata.renderHelperClassDescriptor.runMethodDescriptor)
        .buildWithScope(context) { irFunction ->
            irFunction.createParameterDeclarations()
            val flushFunction = context.symbolTable.referenceFunction(componentMetadata.wrapperViewDescriptor.flushComponentRerenderDescriptor)
            val flushCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
                    -1, -1, context.moduleDescriptor.builtIns.unitType,
                    flushFunction,
                    flushFunction.descriptor
                    , null
            )
            flushCall.dispatchReceiver = IrGetFieldImpl(-1, -1, context.symbolTable.referenceField(viewFieldDescriptor), IrGetValueImpl(-1, -1, renderHelperIrClass.thisReceiver!!.symbol))

            irFunction.body = IrBlockBodyImpl(-1, -1, listOf(flushCall))
        }
    return irFunction
}