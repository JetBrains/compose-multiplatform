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
import org.jetbrains.kotlin.r4a.GeneratedViewClassDescriptor
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.compiler.ir.buildWithScope
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny

fun generateWrapperView(context: GeneratorContext, componentMetadata: ComponentMetadata): IrClass {
    val syntheticClassDescriptor = componentMetadata.wrapperViewDescriptor
    val wrapperViewIrClass = context.symbolTable.declareClass(-1, -1, IrDeclarationOrigin.DEFINED, syntheticClassDescriptor)

    wrapperViewIrClass.createParameterDeclarations()
    wrapperViewIrClass.declarations.add(generateComponentInstanceProperty(context, componentMetadata))
    wrapperViewIrClass.declarations.add(generateConstructor(context, componentMetadata))
    wrapperViewIrClass.declarations.add(generateOnAttachFunction(context, componentMetadata))
    wrapperViewIrClass.declarations.add(generateFlushComponentRerender(context, componentMetadata))
    wrapperViewIrClass.declarations.add(generateRerenderMethod(context, componentMetadata))
    wrapperViewIrClass.declarations.add(generateRerenderHelper(context, componentMetadata))

    return wrapperViewIrClass
}

private fun generateConstructor(context: GeneratorContext, componentMetadata: ComponentMetadata): IrConstructor {
    val syntheticClassDescriptor = componentMetadata.wrapperViewDescriptor
    val constructor = context.symbolTable.declareConstructor(-1, -1, IrDeclarationOrigin.DEFINED, syntheticClassDescriptor.unsubstitutedPrimaryConstructor!!)
        .buildWithScope(context) { constructor ->
            constructor.createParameterDeclarations()
            val wrapperViewAsThisReceiver = context.symbolTable.declareValueParameter(-1, -1, IrDeclarationOrigin.DEFINED, syntheticClassDescriptor.thisAsReceiverParameter).symbol

            val statements = mutableListOf<IrStatement>()
            val superConstructor = context.symbolTable.referenceConstructor(componentMetadata.wrapperViewDescriptor.getSuperClassNotAny()!!.constructors.single{it.valueParameters.size == 1})
            val superCall = IrDelegatingConstructorCallImpl(-1, -1, superConstructor, superConstructor.descriptor, 0)
            superCall.putValueArgument(0, IrGetValueImpl(-1, -1, constructor.valueParameters[0].symbol))
            statements.add(superCall)
            val linearLayoutClass = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.widget.LinearLayout")))!!
            val linearLayoutParamsClass = linearLayoutClass.unsubstitutedMemberScope.getContributedClassifier(Name.identifier("LayoutParams"), NoLookupLocation.FROM_BACKEND)!! as ClassDescriptor
            val linearLayoutParamsConstructor = context.symbolTable.referenceConstructor(linearLayoutParamsClass.constructors.single { it.valueParameters.size == 2 && it.valueParameters[0].type == context.moduleDescriptor.builtIns.intType }!!)

            val layoutParams = IrCallImpl(-1, -1, linearLayoutParamsClass.defaultType,
                                          linearLayoutParamsConstructor,
                                          linearLayoutParamsConstructor.descriptor, null)
            layoutParams.putValueArgument(0, IrGetFieldImpl(-1, -1, context.symbolTable.referenceField(linearLayoutParamsClass.staticScope.getContributedVariables(
                    Name.identifier("MATCH_PARENT"), NoLookupLocation.FROM_BACKEND).single()))
            )
            layoutParams.putValueArgument(1, IrGetFieldImpl(-1, -1, context.symbolTable.referenceField(linearLayoutParamsClass.staticScope.getContributedVariables(
                    Name.identifier("WRAP_CONTENT"), NoLookupLocation.FROM_BACKEND).single()))
            )



            val topLevelComponentCompanion = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(R4aUtils.generateR4APackageName()+".Component")))!!.companionObjectDescriptor!!
            val addWrapperFunction = topLevelComponentCompanion.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("addWrapper"), NoLookupLocation.FROM_BACKEND).single()
            val addWrapperCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
                    -1, -1, context.moduleDescriptor.builtIns.unitType,
                    context.symbolTable.referenceSimpleFunction(addWrapperFunction),
                    addWrapperFunction, null
            )
            addWrapperCall.dispatchReceiver = IrGetObjectValueImpl(-1, -1, topLevelComponentCompanion.defaultType, context.symbolTable.referenceClass(topLevelComponentCompanion))
            addWrapperCall.putValueArgument(0, IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver))
            statements.add(addWrapperCall)



            val setLayoutParamsFunction = linearLayoutClass.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("setLayoutParams"), NoLookupLocation.FROM_BACKEND).single()
            statements.add(IrInstanceInitializerCallImpl(-1, -1, context.symbolTable.referenceClass(syntheticClassDescriptor)))
            val setLayoutParamsCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
                    -1, -1, context.moduleDescriptor.builtIns.unitType,
                    context.symbolTable.referenceSimpleFunction(setLayoutParamsFunction),
                    setLayoutParamsFunction, null
            )
            setLayoutParamsCall.dispatchReceiver = IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver)
            setLayoutParamsCall.putValueArgument(0, layoutParams)
            statements.add(setLayoutParamsCall)



            val componentInstanceProperty = syntheticClassDescriptor.componentInstanceField
            val componentConstructorDescriptor = (componentMetadata.descriptor as ClassDescriptor).unsubstitutedPrimaryConstructor!!
            val componentConstructor = context.symbolTable.referenceConstructor(componentConstructorDescriptor)
            val componentConstructorCall = IrCallImpl(-1, -1, (componentMetadata.descriptor as ClassDescriptor).defaultType, // TODO: rethink cast
                                                      componentConstructor,
                                                      componentConstructorDescriptor, null)
            statements.add(IrSetFieldImpl(-1, -1, context.symbolTable.referenceField(componentInstanceProperty), IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver), componentConstructorCall))

            constructor.body = IrBlockBodyImpl(-1, -1, statements)
        }
    return constructor
}

private fun generateOnAttachFunction(context: GeneratorContext, componentMetadata: ComponentMetadata): IrFunction {
    val syntheticClassDescriptor = componentMetadata.wrapperViewDescriptor
    val functionDescriptor = syntheticClassDescriptor.onAttachDescriptor
    val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
        .buildWithScope(context) { irFunction ->
            irFunction.createParameterDeclarations()
            val wrapperViewAsThisReceiver = irFunction.dispatchReceiverParameter!!.symbol

            val flushFunction = context.symbolTable.referenceFunction(syntheticClassDescriptor.flushComponentRerenderDescriptor)
            val flushCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
                    -1, -1, context.moduleDescriptor.builtIns.unitType,
                    flushFunction,
                    flushFunction.descriptor
                    , null
            )
            flushCall.dispatchReceiver = IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver)

            val linearLayoutClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.widget.LinearLayout")))!!
            val superFunction = linearLayoutClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("onAttachedToWindow"), NoLookupLocation.FROM_BACKEND).single()
            val superCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
                    -1, -1, context.moduleDescriptor.builtIns.unitType,
                    context.symbolTable.referenceSimpleFunction(superFunction),
                    superFunction
                    , null, null, context.symbolTable.referenceClass(syntheticClassDescriptor.getSuperClassNotAny()!!)
            )
            superCall.dispatchReceiver = IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver)

            irFunction.body = IrBlockBodyImpl(-1, -1, listOf(flushCall, superCall))
        }
    return irFunction
}

private fun generateFlushComponentRerender(context: GeneratorContext, componentMetadata: ComponentMetadata): IrFunction {
    val syntheticClassDescriptor = componentMetadata.wrapperViewDescriptor
    val functionDescriptor = syntheticClassDescriptor.flushComponentRerenderDescriptor
    val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
        .buildWithScope(context) { irFunction ->
            irFunction.createParameterDeclarations()
            val wrapperViewAsThisReceiver = irFunction.dispatchReceiverParameter!!.symbol

            val childCountDescriptor = componentMetadata.wrapperViewDescriptor.getSuperClassNotAny()!!.unsubstitutedMemberScope.getContributedFunctions(
                    Name.identifier("getChildCount"), NoLookupLocation.FROM_BACKEND).single {it.valueParameters.size == 0}
            val childCountCall = IrGetterCallImpl(-1, -1,
                                                  context.symbolTable.referenceSimpleFunction(childCountDescriptor),
                                                  childCountDescriptor, 0)
            childCountCall.dispatchReceiver = IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver)

            val renderIntoDescriptor = componentMetadata.renderIntoViewGroupDescriptor
            val renderIntoCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
                    -1, -1, context.moduleDescriptor.builtIns.unitType,
                    context.symbolTable.referenceSimpleFunction(renderIntoDescriptor),
                    renderIntoDescriptor, null
            )
            val componentInstanceField = syntheticClassDescriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("componentInstance"), NoLookupLocation.FROM_BACKEND).single()
            renderIntoCall.putValueArgument(0, IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver))
            renderIntoCall.putValueArgument(1, IrConstImpl.int(-1, -1, context.builtIns.intType, 0))
            renderIntoCall.putValueArgument(2, childCountCall)
            renderIntoCall.dispatchReceiver = IrGetFieldImpl(-1, -1, context.symbolTable.referenceField(componentInstanceField), IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver))

            val exceptionClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("java.lang.Exception")))!!
            val catchVariableDescriptor = LocalVariableDescriptor(irFunction.descriptor, Annotations.EMPTY, Name.identifier("e"), exceptionClassDescriptor.defaultType, SourceElement.NO_SOURCE)
            val ctch = IrCatchImpl(-1, -1, context.symbolTable.declareVariable(-1, -1, IrDeclarationOrigin.CATCH_PARAMETER, catchVariableDescriptor))

            val throwableClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("kotlin.Throwable")))!!
            val runtimeExceptionDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("java.lang.RuntimeException")))!!
            val runtimeExceptionConstructor = runtimeExceptionDescriptor.constructors.single { it.valueParameters.size == 1 && (it.valueParameters[0].type.constructor.declarationDescriptor as ClassDescriptor).classId == throwableClassDescriptor.classId }
            val runtimeExceptionCall = IrCallImpl(-1, -1, runtimeExceptionDescriptor.defaultType,
                                                  context.symbolTable.referenceConstructor(runtimeExceptionConstructor),
                                                  runtimeExceptionConstructor, null)
            runtimeExceptionCall.putValueArgument(0, IrGetValueImpl(-1, -1, context.symbolTable.referenceVariable(catchVariableDescriptor)))

            val irThrow = org.jetbrains.kotlin.ir.expressions.impl.IrThrowImpl(
                    -1,
                    -1,
                    context.builtIns.nothingType,
                    runtimeExceptionCall
            )

            ctch.result = org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl(-1, -1, context.builtIns.unitType, null, listOf(irThrow))

            val tri = org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl(
                    -1,
                    -1,
                    context.builtIns.unitType,
                    org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl(
                            -1,
                            -1,
                            context.builtIns.unitType,
                            null,
                            listOf(renderIntoCall)
                    ),
                    listOf(ctch),
                    null
            )

            irFunction.body = IrBlockBodyImpl(-1, -1, listOf(tri))
        }
    return irFunction
}

private fun generateRerenderMethod(context: GeneratorContext, componentMetadata: ComponentMetadata): IrFunction {
    val syntheticClassDescriptor = componentMetadata.wrapperViewDescriptor
    val functionDescriptor = syntheticClassDescriptor.getRerenderMethodDescriptor()
    val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
        .buildWithScope(context) { irFunction ->
            irFunction.createParameterDeclarations()
            val wrapperViewAsThisReceiver = irFunction.dispatchReceiverParameter!!.symbol

            val looperClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.os.Looper")))!!
            val mainLooperFunction = looperClassDescriptor.staticScope.getContributedFunctions(Name.identifier("getMainLooper"), NoLookupLocation.FROM_BACKEND).single()
            val mainLooperFunctionCall = IrCallImpl(-1, -1, looperClassDescriptor.defaultType,
                                                    context.symbolTable.referenceSimpleFunction(mainLooperFunction),
                                                    mainLooperFunction, null)

            val handlerClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.os.Handler")))!!
            val handlerConstructor = handlerClassDescriptor.constructors.filter { it.valueParameters.size == 1 && (it.valueParameters[0].type.constructor.declarationDescriptor as ClassDescriptor).classId == looperClassDescriptor.classId}.single()
            val handlerConstructorCall = IrCallImpl(-1, -1, handlerClassDescriptor.defaultType,
                                                    context.symbolTable.referenceConstructor(handlerConstructor),
                                                    handlerConstructor, null)
            handlerConstructorCall.putValueArgument(0, mainLooperFunctionCall)

            val helperConstructor = componentMetadata.renderHelperClassDescriptor.unsubstitutedPrimaryConstructor!!
            val helperConstructorCall = IrCallImpl(-1, -1, componentMetadata.renderHelperClassDescriptor.defaultType,
                                                   context.symbolTable.referenceConstructor(helperConstructor),
                                                   helperConstructor, null)
            helperConstructorCall.putValueArgument(0, IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver))

            val postFunction = handlerClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("post"), NoLookupLocation.FROM_BACKEND).single()
            val postFunctionCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
                    -1, -1, context.moduleDescriptor.builtIns.booleanType,
                    context.symbolTable.referenceSimpleFunction(postFunction),
                    postFunction, null
            )
            postFunctionCall.putValueArgument(0, helperConstructorCall)
            postFunctionCall.dispatchReceiver = handlerConstructorCall


            irFunction.body = IrBlockBodyImpl(-1, -1, listOf(postFunctionCall))
        }
    return irFunction;
}


private fun generateComponentInstanceProperty(context: GeneratorContext, componentMetadata: ComponentMetadata) = context.symbolTable.declareField(-1, -1, IrDeclarationOrigin.DEFINED, componentMetadata.wrapperViewDescriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("componentInstance"), NoLookupLocation.FROM_BACKEND).single())