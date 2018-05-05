package org.jetbrains.kotlin.r4a.compiler.lower

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxTag
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.r4a.compiler.ir.buildWithScope
import org.jetbrains.kotlin.r4a.compiler.ir.find
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl

/**
 * Creates synthetics and applies KTX lowering on a class-component.
 */
fun lowerComponentClass(context: GeneratorContext, metadata: ComponentMetadata, component: IrClass) {
    component.declarations.add(generateElementsHolderField(context, component)) // TODO: should not need component
    component.declarations.add(generateRenderIntoViewGroupHelperFunction(context, component)) // TODO: should not need component
    component.declarations.add(generateWrapperView(context, metadata, component)) // TODO: should not need component
    component.declarations.add(generateComponentCompanionObject(context, metadata, component)) // TODO: should not need component

    lowerComposeFunction(context, component)
}

private fun transform(context: GeneratorContext, component: IrClass, function: IrFunction, tag: IrKtxTag): List<IrStatement> {
    val output: MutableList<IrStatement> = mutableListOf()

    val element = tag.element // TODO: Should transform pure IR rather than relying on the element

    val tagType = context.bindingContext.get(
            R4AWritableSlices.KTX_TAG_TYPE_DESCRIPTOR,
            element
    ) as ClassifierDescriptor? ?: throw NullPointerException("KTX tag does not know descriptor: " + element.text)


    val getMarkupBuilderFunction = component.descriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("markupBuilder"), NoLookupLocation.FROM_BACKEND).single().getter!!
    val getMarkupBuilderFunctionSymbol = context.symbolTable.referenceSimpleFunction(getMarkupBuilderFunction)
    val getMarkupBuilderCall = IrGetterCallImpl(tag.startOffset, tag.endOffset,
                                                getMarkupBuilderFunctionSymbol,
                                                getMarkupBuilderFunction, 0)
    getMarkupBuilderCall.dispatchReceiver = IrGetValueImpl(
            tag.startOffset, tag.endOffset,
            component.thisReceiver!!.symbol
    )

    val klassConstructor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("kotlin.reflect.KClass")))!!.typeConstructor
    val klassSymbol = context.symbolTable.referenceClass(tagType as ClassDescriptor)
    val tagKlassReference = IrClassReferenceImpl(tag.startOffset, tag.endOffset, KotlinTypeFactory.simpleType(
            Annotations.EMPTY, klassConstructor, listOf(
            TypeProjectionImpl(tagType.defaultType)
    ), false), klassSymbol, tagType.defaultType)


    val dslBuilderClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(R4aUtils.generateR4APackageName()+".R4aElementBuilderDSL")))!!

    val startComponentMethod = dslBuilderClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("startComponent"), NoLookupLocation.FROM_BACKEND).single {(it as? SimpleFunctionDescriptor)?.valueParameters!![0].type.constructor.declarationDescriptor!!.name.identifier == "KClass"}
    val startComponentMethodCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
            tag.startOffset, tag.endOffset, context.moduleDescriptor.builtIns.unitType,
            context.symbolTable.referenceSimpleFunction(startComponentMethod),
            startComponentMethod, null
    )
    startComponentMethodCall.putValueArgument(0, tagKlassReference)
    startComponentMethodCall.dispatchReceiver = getMarkupBuilderCall
    output.add(startComponentMethodCall)


    val attributeMethod = dslBuilderClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("attribute"), NoLookupLocation.FROM_BACKEND).single()
    for(attribute in tag.attributes) {

        val attributeMethodCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
                attribute.startOffset, attribute.endOffset, context.moduleDescriptor.builtIns.unitType,
                context.symbolTable.referenceSimpleFunction(attributeMethod),
                attributeMethod, null
        )
        attributeMethodCall.putValueArgument(0, IrConstImpl.string(attribute.startOffset, attribute.endOffset, context.builtIns.stringType, attribute.element.key!!.text))
        val value =
            if(attribute.element.isStaticStringValue) {
                val stringConstant = attribute.element.value!!.text.let { it.substring(1, it.length-1) };
                IrConstImpl.string(attribute.element.value!!.startOffset, attribute.element.value!!.endOffset, context.builtIns.stringType, stringConstant)
            }
            else attribute.value
        attributeMethodCall.putValueArgument(0, IrConstImpl.string(attribute.element.key!!.startOffset, attribute.element.key!!.endOffset, context.builtIns.stringType, attribute.element.key!!.text))
        attributeMethodCall.putValueArgument(1, value)
        attributeMethodCall.dispatchReceiver = getMarkupBuilderCall

        output.add(attributeMethodCall)
    }

    if(tag.body != null)
        for(statement in tag.body) {
            if(statement is IrKtxTag) output.addAll(transform(context, component, function, statement))
            else output.add(statement)
        }

    val endComponentMethod = dslBuilderClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("endComponent"), NoLookupLocation.FROM_BACKEND).single()
    val endComponentMethodCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
            tag.startOffset, tag.endOffset, context.moduleDescriptor.builtIns.unitType,
            context.symbolTable.referenceSimpleFunction(endComponentMethod),
            endComponentMethod, null
    )
    endComponentMethodCall.dispatchReceiver = getMarkupBuilderCall
    output.add(endComponentMethodCall)

    return output
}

fun generateElementsHolderField(context: GeneratorContext, component: IrClass): IrField {
    return context.symbolTable.declareField(-1, -1, IrDeclarationOrigin.DEFINED, ComponentMetadata.fromDescriptor(component.descriptor).elementsFieldDescriptor)
}

private fun lowerComposeFunction(context: GeneratorContext, component: IrClass) {
    val compose = component.find { it is IrSimpleFunction && it.name.toString() == "compose" && it.descriptor.valueParameters.size == 0 }.single() as IrSimpleFunction

    // Insert some code at the beginning of compose, that initializes the markup builder
    val dslBuilderClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(R4aUtils.generateR4APackageName()+".R4aElementBuilderDSL")))!!
    val dslBuilderConstructor = context.symbolTable.referenceConstructor(dslBuilderClassDescriptor.unsubstitutedPrimaryConstructor!!)
    val dslBuilderIrConstructor = IrCallImpl(-1, -1, dslBuilderClassDescriptor.defaultType,
                                             dslBuilderConstructor,
                                             dslBuilderConstructor.descriptor, null)

    val setMarkupBuilderFunction = component.descriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("markupBuilder"), NoLookupLocation.FROM_BACKEND).single().setter!!
    val setMarkupBuilderFunctionSymbol = context.symbolTable.referenceSimpleFunction(setMarkupBuilderFunction)
    val setMarkupBuilderCall = IrSetterCallImpl(-1, -1,
                                                setMarkupBuilderFunctionSymbol,
                                                setMarkupBuilderFunction, 0)
    setMarkupBuilderCall.putValueArgument(0, dslBuilderIrConstructor)
    setMarkupBuilderCall.dispatchReceiver = IrGetValueImpl(
            -1, -1,
            component.thisReceiver!!.symbol
    )

    (compose.body as IrBlockBody).statements.add(0, setMarkupBuilderCall)


    // Transform the KTX tags within compose
    compose.body!!.accept(object : IrElementTransformer<Nothing?> {
        override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?): IrElement {
            val block = IrBlockImpl(expression.startOffset, expression.endOffset, context.moduleDescriptor.builtIns.unitType, object: IrStatementOriginImpl("KTX Tag"){}, transform(context, component, compose, expression as IrKtxTag))
            block.accept(this, data)
            return block;
        }
    }, null)
}

fun generateRenderIntoViewGroupHelperFunction(context: GeneratorContext, component: IrClass): IrFunction {
    val metadata = ComponentMetadata.fromDescriptor(component.descriptor)
    val functionDescriptor = metadata.renderIntoViewGroupDescriptor
    val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
        .buildWithScope(context) { irFunction ->

            irFunction.createParameterDeclarations()

            val statements = mutableListOf<IrStatement>()


            val renderFunction = context.symbolTable.referenceFunction(component.descriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("compose"), NoLookupLocation.FROM_BACKEND).single())
            val renderCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                        renderFunction,
                                        renderFunction.descriptor
                                        , null)
            renderCall.dispatchReceiver = IrGetValueImpl(-1, -1, component.thisReceiver!!.symbol)
            statements.add(renderCall)


            val getMarkupBuilderFunction = component.descriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("markupBuilder"), NoLookupLocation.FROM_BACKEND).single().getter!!
            val getMarkupBuilderCall = IrGetterCallImpl(-1, -1,
                                                        context.symbolTable.referenceSimpleFunction(getMarkupBuilderFunction),
                                                        getMarkupBuilderFunction, 0)
            getMarkupBuilderCall.dispatchReceiver = IrGetValueImpl(-1, -1,component.thisReceiver!!.symbol)

            val dslBuilderClassDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(R4aUtils.generateR4APackageName()+".R4aElementBuilderDSL")))!!

            val renderResultsGetterDescriptor = dslBuilderClassDescriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier("renderResults"), NoLookupLocation.FROM_BACKEND).single().getter!!
            val renderResultsMethodCall = IrGetterCallImpl(-1, -1,
                                                           context.symbolTable.referenceSimpleFunction(renderResultsGetterDescriptor),
                                                           renderResultsGetterDescriptor, 0)
            renderResultsMethodCall.dispatchReceiver = getMarkupBuilderCall

            val r4aDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(R4aUtils.generateR4APackageName()+".R4a")))!!
            val staticRenderIntoDescriptor = r4aDescriptor.staticScope.getContributedFunctions(Name.identifier("renderIntoWrapperView"), NoLookupLocation.FROM_BACKEND).single()
            val staticRenderIntoCall = IrCallImpl(-1, -1, context.moduleDescriptor.builtIns.unitType,
                                                  context.symbolTable.referenceSimpleFunction(staticRenderIntoDescriptor),
                                                  staticRenderIntoDescriptor, null)
            staticRenderIntoCall.putValueArgument(0, IrGetFieldImpl(-1, -1, context.symbolTable.referenceField(metadata.elementsFieldDescriptor), IrGetValueImpl(-1, -1, component.thisReceiver!!.symbol)))
            staticRenderIntoCall.putValueArgument(1, renderResultsMethodCall)
            staticRenderIntoCall.putValueArgument(2, IrGetValueImpl(-1, -1, irFunction.valueParameters[0].symbol))
            staticRenderIntoCall.putValueArgument(3, IrGetValueImpl(-1, -1, irFunction.valueParameters[1].symbol))
            staticRenderIntoCall.putValueArgument(4, IrGetValueImpl(-1, -1, irFunction.valueParameters[2].symbol))
            statements.add(staticRenderIntoCall)

            statements.add(IrSetFieldImpl(-1, -1, context.symbolTable.referenceField(metadata.elementsFieldDescriptor), IrGetValueImpl(-1, -1, component.thisReceiver!!.symbol), renderResultsMethodCall))

            irFunction.body = IrBlockBodyImpl(-1, -1, statements)
        }
    return irFunction
}


