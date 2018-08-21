package org.jetbrains.kotlin.r4a.compiler.lower

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl.Companion.constTrue
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.r4a.KtxAttributeInfo
import org.jetbrains.kotlin.r4a.KtxTagInfo
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.analysis.ComposableType
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxTag
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

/**
 * Creates synthetics and applies KTX lowering on a class-component.
 */
fun lowerComponentClass(context: GeneratorContext, metadata: ComponentMetadata, component: IrClass) {
    // TODO: if (component.annotations.includes(GenerateWrapperViewAnnotation) { ...
  //  component.declarations.add(generateWrapperView(context, metadata))
    // }

    component.declarations.add(generateComponentCompanionObject(context, metadata))
}

class IrAttributeInfo(
    val name: String,
    val info: KtxAttributeInfo,
    val getValue: IrGetValue
)

/**
 * This function is an IR transformation of any KTX tag.
 *
 * Roughly speaking, this takes the following KTX:
 *
 *     <Foo bar={expr1} bam={expr2}>
 *         ...expr3...
 *     </Foo>
 *
 * and turns it into the following code:
 *
 *     // a bool that we use to determine if we should continue diving down the tree
 *     var run = false
 *     val attrBar = expr1
 *     val attrBam = expr2
 *
 *     // we generate a `sourceKey` in this transform function which is meant to be a key that is
 *     // unique to the source location of the KTX element. We also add pivotal parameters to this key
 *     cc.start(cc.ck(sourceKey, attrBar))
 *     val tmpEl = if (cc.isInserting()) cc.setInstance(Foo(attrBar)) else cc.getInstance()
 *
 *     // we then iterate over each non-pivotal attribute, setting only the ones that have changed
 *     if (cc.attributeChangedOrEmpty(attrBam)) {
 *         run = true
 *         tmpEl.setBam(attrBam)
 *     }
 *
 *     // if there is a body for this element, it will execute that code as normal, including any
 *     // KTX statements that are a part of it
 *     ...expr3...
 *
 *     // if the element is a component, we recurse down:
 *     if (run) {
 *         tmpEl.compose()
 *     }
 *
 *     // now we are done with the element
 *     cc.end()
 *
 */
private fun transform(
    context: GeneratorContext,
    container: IrPackageFragment,
    owner: DeclarationDescriptor,
    tag: IrKtxTag,
    helper: ComposeFunctionHelper
): List<IrStatement> {
    val tagIndex = helper.uniqueIndex()
    val output: MutableList<IrStatement> = mutableListOf()

    val info = context.bindingContext.get(R4AWritableSlices.KTX_TAG_INFO, tag.element) ?: error("No tagInfo found on element")

    // OUTPUT: run = false
    output.add(helper.setRun(false))

    val attributes = tag.attributes.map { irAttr ->
        val attrInfo = context.bindingContext.get(R4AWritableSlices.KTX_ATTR_INFO, irAttr.element)
                ?: error("Attribute missing KTX_ATTR_INFO context")
        val name = attrInfo.name

        val attrVariable = IrTemporaryVariableDescriptorImpl(
            helper.compose.descriptor,
            Name.identifier("__el_attr_${tagIndex}_$name"),
            attrInfo.type,
            false
        )

        val attrVariableDeclaration = context.symbolTable.declareVariable(
            irAttr.startOffset, irAttr.endOffset,
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            attrVariable,
            attrInfo.type.toIrType()!!,
            irAttr.value
        )

        // OUTPUT: val _el_attrName = (...attrExpression...)
        output.add(attrVariableDeclaration)

        val getValue = IrGetValueImpl(
            irAttr.startOffset, irAttr.endOffset,
            context.symbolTable.referenceVariable(attrVariable)
        )

        IrAttributeInfo(
            name = name,
            info = attrInfo,
            getValue = getValue
        )
    }
    val childrenInfo = info.childrenInfo
    val irChildrenInfo = if (childrenInfo != null && info.composableType != ComposableType.VIEW) {
        val tagBody = tag.body ?: error("expected tag body")
        val lambda = tag.element.bodyLambdaExpression ?: error("expected lambda")
        val parameters = lambda.valueParameters
        val parameterDescriptors = parameters.mapNotNull {
            context.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, it) as? ValueParameterDescriptor
        }

        val childrenLambdaIrClass = generateChildrenLambda(
            context,
            container,
            tag.capturedExpressions,
            childrenInfo.type,
            parameterDescriptors,
            tagBody
        )

        lowerComposeFunction(
            context,
            container,
            childrenLambdaIrClass.declarations.single { it is IrFunctionImpl && it.name.identifier == "invoke" } as IrFunction
        )

        container.declarations.add(childrenLambdaIrClass)

        val lambdaConstructor = (childrenLambdaIrClass.declarations.single { it is IrConstructor } as IrConstructorImpl)
        val lambdaConstructorCall = IrCallImpl(
            -1, -1,
            childrenLambdaIrClass.defaultType,
            lambdaConstructor.symbol
        ).apply {
            tag.capturedExpressions.forEachIndexed { index, value ->
                putValueArgument(index, value)
            }
        }

        val name = childrenInfo.name

        val attrVariable = IrTemporaryVariableDescriptorImpl(
            helper.compose.descriptor,
            Name.identifier("__el_chld_${tagIndex}_$name"),
            childrenInfo.type,
            false
        )

        val attrVariableDeclaration = context.symbolTable.declareVariable(
            -1, -1,
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            attrVariable,
            lambdaConstructorCall.type,
            lambdaConstructorCall
        )

        // OUTPUT: val _el_attrName = (...attrExpression...)
        output.add(attrVariableDeclaration)

        val getValue = IrGetValueImpl(
            -1, -1,
            context.symbolTable.referenceVariable(attrVariable)
        )

        IrAttributeInfo(
            name = name,
            info = childrenInfo,
            getValue = getValue
        )
    } else null

    val attributesByName = attributes.map { it.name to it }.toMap()

    tag.callExpr.apply {
        info.parameterInfos.forEachIndexed { index, attrInfo ->
            if (attrInfo.isChildren) {
                if (info.isConstructed) {
                    // TODO(lmr): if children is part of the constructor, pass it in here. Need to wait for property support for this
                    error("constructor children properties not yet supported")
                } else {
                    putValueArgument(index, irChildrenInfo?.getValue)
                }
            } else if (attrInfo.isContext) {
                putValueArgument(index, helper.getAndroidContextCall)
            } else {
                putValueArgument(index, attributesByName[attrInfo.name]?.getValue)
            }
        }
    }

    when (info.composableType) {
        ComposableType.COMPONENT -> transformComponentElement(
            context,
            container,
            owner,
            tag,
            helper,
            output,
            info,
            attributes,
            irChildrenInfo,
            tagIndex
        )
        ComposableType.VIEW -> transformViewElement(
            context,
            container,
            owner,
            tag,
            helper,
            output,
            info,
            attributes,
            tagIndex
        )
        ComposableType.FUNCTION_VAR -> transformFunctionVar(
            context,
            container,
            owner,
            tag,
            helper,
            output,
            info,
            attributes,
            irChildrenInfo,
            tagIndex
        )
        ComposableType.FUNCTION -> transformFunctionComponent(
            context,
            container,
            owner,
            tag,
            helper,
            output,
            info,
            attributes,
            irChildrenInfo,
            tagIndex
        )
        ComposableType.UNKNOWN -> error("Unknown composable type encountered")
    }

    return output
}

private enum class GroupKind {
    Component,
    View,
    Function,
}

private fun callStart(
    kind: GroupKind,
    attributes: Collection<IrAttributeInfo>,
    helper: ComposeFunctionHelper,
    tag: IrKtxTag,
    context: GeneratorContext,
    tagIndex: Int,
    output: MutableList<IrStatement>
) {
    val pivotalAttributes = attributes.filter { it.info.isPivotal || it.name == "key" }

    val ccStartMethod = when (kind) {
        GroupKind.View -> if (pivotalAttributes.isNotEmpty()) helper.ccStartViewWithKey else helper.ccStartViewWithoutKey
        else -> if (pivotalAttributes.isNotEmpty()) helper.ccStartMethodWithKey else helper.ccStartMethodWithoutKey
    }

    val ccStartMethodCall = IrCallImpl(
        tag.startOffset, tag.endOffset,
        ccStartMethod.returnType!!.toIrType()!!,
        context.symbolTable.referenceFunction(ccStartMethod)
    ).apply {
        dispatchReceiver = helper.getCc
        putValueArgument(
            0,
            IrConstImpl.int(
                tag.startOffset,
                tag.endOffset,
                context.irBuiltIns.intType,
                "${helper.functionDescription}::$tagIndex".hashCode()
            )
        )
        if (pivotalAttributes.isNotEmpty()) {
            putValueArgument(1, constructKey(context, helper, pivotalAttributes))
        }
    }

    // OUTPUT: cc.start(...)
    output.add(ccStartMethodCall)
}

private fun constructKey(context: GeneratorContext, helper: ComposeFunctionHelper, attributes: List<IrAttributeInfo>): IrExpression {
    // if we just have
    return if (attributes.size == 1) attributes.single().getValue
    else IrCallImpl(
        -1, -1,
        context.irBuiltIns.anyNType,
        context.symbolTable.referenceFunction(helper.ccJoinKeyMethod)
    ).apply {
        dispatchReceiver = helper.getCc
        // the composite key should be a balanced binary tree of "JoinedKeys" to minimize comparisons, so we recursively split the list
        // of pivotal attributes in half
        val midpoint = attributes.size / 2
        val left = attributes.subList(0, midpoint)
        val right = attributes.subList(midpoint, attributes.size)
        putValueArgument(0, constructKey(context, helper, left))
        putValueArgument(1, constructKey(context, helper, right))
    }
}

// OUTPUT: cc.end()
private fun callEnd(
    output: MutableList<IrStatement>,
    helper: ComposeFunctionHelper
) {
    output.add(helper.ccEndMethodCall)
}



/**
 * Function vars never memoize, so we don't bother with `run` here. We just invoke
 */
private fun transformFunctionVar(
    context: GeneratorContext,
    container: IrPackageFragment,
    owner: DeclarationDescriptor,
    tag: IrKtxTag,
    helper: ComposeFunctionHelper,
    output: MutableList<IrStatement>,
    info: KtxTagInfo,
    attributes: Collection<IrAttributeInfo>,
    childrenInfo: IrAttributeInfo?,
    tagIndex: Int
) {
    output.add(tag.callExpr)
}

/**
 * Functions can memoize, so we check each attribute and skip if we can
 */
private fun transformFunctionComponent(
    context: GeneratorContext,
    container: IrPackageFragment,
    owner: DeclarationDescriptor,
    tag: IrKtxTag,
    helper: ComposeFunctionHelper,
    output: MutableList<IrStatement>,
    info: KtxTagInfo,
    attributes: Collection<IrAttributeInfo>,
    childrenInfo: IrAttributeInfo?,
    tagIndex: Int
) {
    callStart(GroupKind.Function, attributes, helper, tag, context, tagIndex, output)
    val startOffset = -1
    val endOffset = -1

    for (attr in attributes) {

        val conditionExpr = IrCallImpl(
            startOffset, endOffset,
            helper.ccAttributeChangedOrInsertingFunctionDescriptor.returnType!!.toIrType()!!,
            context.symbolTable.referenceFunction(helper.ccAttributeChangedOrInsertingFunctionDescriptor)
        ).apply {
            dispatchReceiver = helper.getCc
            putValueArgument(0, attr.getValue)
        }

        val ifExpr = IrIfThenElseImpl(
            startOffset, endOffset,
            context.irBuiltIns.unitType
        ).apply {
            branches.add(IrBranchImpl(startOffset, endOffset, conditionExpr, helper.setRun(true)))
        }

        // OUTPUT: if (cc.attributeChangedOrInserting(attr)) run = true
        output.add(ifExpr)
    }

    if (childrenInfo != null) {
        // NOTE(lmr): right now children lambdas will always pierce memoization, but we could codegen an equals function that didn't.
        val conditionExpr = IrCallImpl(
            startOffset, endOffset,
            helper.ccAttributeChangedOrInsertingFunctionDescriptor.returnType!!.toIrType()!!,
            context.symbolTable.referenceFunction(helper.ccAttributeChangedOrInsertingFunctionDescriptor)
        ).apply {
            dispatchReceiver = helper.getCc
            putValueArgument(0, childrenInfo.getValue)
        }

        val ifExpr = IrIfThenElseImpl(
            startOffset, endOffset,
            context.irBuiltIns.unitType
        ).apply {
            branches.add(IrBranchImpl(startOffset, endOffset, conditionExpr, helper.setRun(true)))
        }

        // OUTPUT: if (cc.attributeChangedOrInserting(attr)) run = true
        output.add(ifExpr)
    }

    val ifRunThenComposeExpr = IrIfThenElseImpl(
        startOffset, endOffset,
        context.irBuiltIns.unitType
    ).apply {
        branches.add(IrBranchImpl(startOffset, endOffset, helper.getRun, tag.callExpr))
    }

    output.add(ifRunThenComposeExpr)

    callEnd(output, helper)
}

private fun transformComponentElement(
    context: GeneratorContext,
    container: IrPackageFragment,
    owner: DeclarationDescriptor,
    tag: IrKtxTag,
    helper: ComposeFunctionHelper,
    output: MutableList<IrStatement>,
    info: KtxTagInfo,
    attributes: Collection<IrAttributeInfo>,
    childrenInfo: IrAttributeInfo?,
    tagIndex: Int
) {
    callStart(GroupKind.Component, attributes, helper, tag, context, tagIndex, output)
    val instanceType = info.instanceType ?: error("expected instance type")

    val startOffset = -1
    val endOffset = -1

    val elVariable = IrTemporaryVariableDescriptorImpl(
        helper.compose.descriptor,
        Name.identifier("__elc$tagIndex"),
        instanceType,
        false
    )

    val elVariableDeclaration = context.symbolTable.declareVariable(
        startOffset, endOffset,
        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        elVariable,
        elVariable.type.toIrType()!!
    )

    val elSymbol = context.symbolTable.referenceVariable(elVariable)

    val getEl = IrGetValueImpl(
        startOffset,
        endOffset,
        elSymbol
    )

    // OUTPUT: val el: InstanceType
    output.add(elVariableDeclaration)

    val assignElExpr = IrSetVariableImpl(
        startOffset, endOffset,
        elSymbol.descriptor.type.toIrType()!!,
        elSymbol,
        tag.callExpr,
        KTX_TAG_ORIGIN
    )

    val callSetInstanceExpr = IrCallImpl(
        startOffset, endOffset,
        helper.ccSetInstanceFunctionDescriptor.returnType!!.toIrType()!!,
        context.symbolTable.referenceFunction(helper.ccSetInstanceFunctionDescriptor)
    ).apply {
        dispatchReceiver = helper.getCc
        putValueArgument(0, getEl)
    }

    val thenBranchExpr = IrBlockImpl(
        startOffset, endOffset,
        context.irBuiltIns.unitType,
        KTX_TAG_ORIGIN,
        listOf(
            helper.setRun(true),
            assignElExpr,
            callSetInstanceExpr
        )
    )

    val elseBranchExpr = IrSetVariableImpl(
        startOffset, endOffset,
        elSymbol.descriptor.type.toIrType()!!,
        elSymbol,
        helper.getUseInstanceCall(instanceType),
        KTX_TAG_ORIGIN
    )

    val ifElseExpr = IrIfThenElseImpl(
        startOffset, endOffset,
        context.irBuiltIns.unitType
    ).apply {
        branches.add(IrBranchImpl(startOffset, endOffset, helper.isInsertingCall, thenBranchExpr))
        branches.add(IrElseBranchImpl(startOffset, endOffset, constTrue(startOffset, endOffset, context.irBuiltIns.booleanType), elseBranchExpr))
    }

    // OUTPUT:
    // if (cc.inserting())
    //     run = true
    //     el = Foo(...)
    //     cc.setInstance(el)
    // else
    //     el = cc.getInstance()
    output.add(ifElseExpr)

    for (attribute in attributes) {

        /**
         *     // we then iterate over each non-pivotal attribute, setting only the ones that have changed
         *     if (cc.attributeChangedOrEmpty(attrBam)) {
         *         run = true
         *         tmpEl.setBam(attrBam)
         *     }
         */

        val resolvedCall = attribute.info.setterResolvedCall ?: continue

        val checkFunction = if (attribute.info.isIncludedInConstruction) helper.ccAttributeChangedFunctionDescriptor
        else helper.ccAttributeChangedOrInsertingFunctionDescriptor

        val callUpdateAttributeExpr = IrCallImpl(
            startOffset, endOffset,
            checkFunction.returnType!!.toIrType()!!,
            context.symbolTable.referenceFunction(checkFunction)
        ).apply {
            dispatchReceiver = helper.getCc
            putValueArgument(0, attribute.getValue)
        }

        val attributeSetterCall = IrCallImpl(
            startOffset, endOffset,
            context.irBuiltIns.unitType,
            context.symbolTable.referenceFunction(resolvedCall.resultingDescriptor.original),
            resolvedCall.resultingDescriptor as FunctionDescriptor,
            resolvedCall.typeArguments.size,
            IrStatementOrigin.INVOKE,
            null
        ).apply {
            resolvedCall.extensionReceiver?.let {
                extensionReceiver = getEl
            }
            resolvedCall.dispatchReceiver?.let {
                if (it.type == instanceType) {
                    dispatchReceiver = getEl
                } else {
                    // this could be a local extension function... we might want to support this?
                    error("unknown dispatch receiver")
                }
            }
            putValueArgument(0, attribute.getValue)
        }

        val thenUpdBranchExpr = IrBlockImpl(
            startOffset, endOffset,
            context.irBuiltIns.unitType,
            KTX_TAG_ORIGIN,
            listOf(
                helper.setRun(true),
                attributeSetterCall
            )
        )

        val updateIfNeededExpr = IrIfThenElseImpl(
            startOffset, endOffset,
            context.irBuiltIns.unitType
        ).apply {
            branches.add(IrBranchImpl(startOffset, endOffset, callUpdateAttributeExpr, thenUpdBranchExpr))
        }

        // OUTPUT:
        // if (cc.attributeChangedOrEmpty(attrBam)) {
        //    run = true
        //    tmpEl.setBam(attrBam)
        // }
        output.add(updateIfNeededExpr)
    }

    childrenInfo?.let { attribute ->
        val resolvedCall = attribute.info.setterResolvedCall ?: return@let

        val checkFunction = if (attribute.info.isIncludedInConstruction) helper.ccAttributeChangedFunctionDescriptor
        else helper.ccAttributeChangedOrInsertingFunctionDescriptor

        val callUpdateAttributeExpr = IrCallImpl(
            startOffset, endOffset,
            checkFunction.returnType!!.toIrType()!!,
            context.symbolTable.referenceFunction(checkFunction)
        ).apply {
            dispatchReceiver = helper.getCc
            putValueArgument(0, attribute.getValue)
        }

        val attributeSetterCall = IrCallImpl(
            startOffset, endOffset,
            context.irBuiltIns.unitType,
            context.symbolTable.referenceFunction(resolvedCall.resultingDescriptor.original),
            resolvedCall.resultingDescriptor as FunctionDescriptor,
            resolvedCall.typeArguments.size,
            IrStatementOrigin.INVOKE,
            null
        ).apply {
            resolvedCall.extensionReceiver?.let {
                extensionReceiver = getEl
            }
            resolvedCall.dispatchReceiver?.let {
                if (it.type == instanceType) {
                    dispatchReceiver = getEl
                } else {
                    // this could be a local extension function... we might want to support this?
                    error("unknown dispatch receiver")
                }
            }
            putValueArgument(0, attribute.getValue)
        }

        val thenUpdBranchExpr = IrBlockImpl(
            startOffset, endOffset,
            context.irBuiltIns.unitType,
            KTX_TAG_ORIGIN,
            listOf(
                helper.setRun(true),
                attributeSetterCall
            )
        )

        val updateIfNeededExpr = IrIfThenElseImpl(
            startOffset, endOffset,
            context.irBuiltIns.unitType
        ).apply {
            branches.add(IrBranchImpl(startOffset, endOffset, callUpdateAttributeExpr, thenUpdBranchExpr))
            branches.add(IrElseBranchImpl(startOffset, endOffset, constTrue(startOffset, endOffset, context.irBuiltIns.booleanType), elseBranchExpr))
        }

        // OUTPUT:
        // if (cc.attributeChangedOrEmpty(attrBam)) {
        //    run = true
        //    tmpEl.setBam(attrBam)
        // }
        output.add(updateIfNeededExpr)
    }

    /**
     *     // if the element is a component, we recurse down:
     *     if (run) {
     *         cc.willCompose()
     *         tmpEl.compose()
     *     }
     */

    val composeExpr = IrCallImpl(
        startOffset, endOffset,
        helper.componentComposeMethod.returnType!!.toIrType()!!,
        context.symbolTable.referenceFunction(helper.componentComposeMethod)
    ).apply {
        dispatchReceiver = getEl
    }

    val ifRunThenComposeExpr = IrIfThenElseImpl(
        startOffset, endOffset,
        context.irBuiltIns.unitType
    ).apply {
        branches.add(IrBranchImpl(startOffset, endOffset, helper.getRun, IrBlockImpl(
            startOffset, endOffset,
            context.irBuiltIns.unitType,
            KTX_TAG_ORIGIN,
            listOf(
                helper.ccWillComposeMethodCall,
                composeExpr
            )
        )))
    }

    output.add(ifRunThenComposeExpr)

    callEnd(output, helper)
}

private fun transformViewElement(
    context: GeneratorContext,
    container: IrPackageFragment,
    owner: DeclarationDescriptor,
    tag: IrKtxTag,
    helper: ComposeFunctionHelper,
    output: MutableList<IrStatement>,
    info: KtxTagInfo,
    attributes: Collection<IrAttributeInfo>,
    tagIndex: Int
) {
    callStart(GroupKind.View, attributes, helper, tag, context, tagIndex, output)
    val instanceType = info.instanceType ?: error("expected instance type")

    val startOffset = -1
    val endOffset = -1

    val elVariable = IrTemporaryVariableDescriptorImpl(
        helper.compose.descriptor,
        Name.identifier("__elv$tagIndex"),
        instanceType,
        false
    )

    val elVariableDeclaration = context.symbolTable.declareVariable(
        startOffset, endOffset,
        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        elVariable,
        elVariable.type.toIrType()!!
    )

    val elSymbol = context.symbolTable.referenceVariable(elVariable)

    val getEl = IrGetValueImpl(
        startOffset,
        endOffset,
        elSymbol
    )

    // OUTPUT: val el: InstanceType
    output.add(elVariableDeclaration)

    val assignElExpr = IrSetVariableImpl(
        startOffset, endOffset,
        elSymbol.descriptor.type.toIrType()!!,
        elSymbol,
        tag.callExpr,
        KTX_TAG_ORIGIN
    )

    val callSetInstanceExpr = IrCallImpl(
        startOffset, endOffset,
        helper.ccSetInstanceFunctionDescriptor.returnType!!.toIrType()!!,
        context.symbolTable.referenceFunction(helper.ccSetInstanceFunctionDescriptor)
    ).apply {
        dispatchReceiver = helper.getCc
        putValueArgument(0, getEl)
    }

    val thenBranchExpr = IrBlockImpl(
        startOffset, endOffset,
        context.irBuiltIns.unitType,
        KTX_TAG_ORIGIN,
        listOf(
            assignElExpr,
            callSetInstanceExpr
        )
    )

    val elseBranchExpr = IrSetVariableImpl(
        startOffset, endOffset,
        elSymbol.descriptor.type.toIrType()!!,
        elSymbol,
        helper.getUseInstanceCall(instanceType),
        KTX_TAG_ORIGIN
    )

    val ifElseExpr = IrIfThenElseImpl(
        startOffset, endOffset,
        context.irBuiltIns.unitType

    ).apply {
        branches.add(IrBranchImpl(startOffset, endOffset, helper.isInsertingCall, thenBranchExpr))
        branches.add(IrElseBranchImpl(startOffset, endOffset, constTrue(startOffset, endOffset, context.irBuiltIns.booleanType), elseBranchExpr))
    }

    // OUTPUT:
    // if (cc.inserting())
    //     el = Foo(...)
    //     cc.setInstance(el)
    // else
    //     el = cc.getInstance()
    output.add(ifElseExpr)

    for (attribute in attributes) {

        /**
         *     // we then iterate over each non-pivotal attribute, setting only the ones that have changed
         *     if (cc.attributeChangedOrEmpty(attrBam)) {
         *         tmpEl.setBam(attrBam)
         *     }
         */

        val resolvedCall = attribute.info.setterResolvedCall ?: continue

        val checkFunction = if (attribute.info.isIncludedInConstruction) helper.ccAttributeChangedFunctionDescriptor
        else helper.ccAttributeChangedOrInsertingFunctionDescriptor

        val callUpdateAttributeExpr = IrCallImpl(
            startOffset, endOffset,
            checkFunction.returnType!!.toIrType()!!,
            context.symbolTable.referenceFunction(checkFunction)
        ).apply {
            dispatchReceiver = helper.getCc
            putValueArgument(0, attribute.getValue)
        }

        val attributeSetterCall = IrCallImpl(
            startOffset, endOffset,
            resolvedCall.resultingDescriptor.returnType!!.toIrType()!!,
            context.symbolTable.referenceFunction(resolvedCall.resultingDescriptor)
        ).apply {
            resolvedCall.extensionReceiver?.let {
                extensionReceiver = getEl
            }
            resolvedCall.dispatchReceiver?.let {
                if (it.type == instanceType) {
                    dispatchReceiver = getEl
                } else {
                    // this could be a local extension function... we might want to support this?
                    error("unknown dispatch receiver")
                }
            }
            putValueArgument(0, attribute.getValue)
        }

        val updateIfNeededExpr = IrIfThenElseImpl(
            startOffset, endOffset,
            context.irBuiltIns.unitType
        ).apply {
            branches.add(IrBranchImpl(startOffset, endOffset, callUpdateAttributeExpr, attributeSetterCall))
        }

        // OUTPUT:
        // if (cc.attributeChangedOrEmpty(attrBam)) {
        //    tmpEl.setBam(attrBam)
        // }
        output.add(updateIfNeededExpr)
    }

    tag.body?.let { body ->
        for (statement in body) {
            if (statement is IrKtxTag) output.addAll(transform(context, container, owner, statement, helper))
            else output.add(statement)
        }
    }

    callEnd(output, helper)
}

fun lowerComposeFunction(context: GeneratorContext, container: IrPackageFragment, compose: IrFunction) {
    context.symbolTable.withScope(compose.descriptor) {

        val helper = ComposeFunctionHelper(context, compose)
        // at the beginning of every compose function, we store CompositionContext.current into a local variable. This is just temporary
        // until we figure out the best way to properly thread context through. This current design is assuming everything is done on the
        // main thread, so we have some wiggle room.

        val ccVariableDeclaration = context.symbolTable.declareVariable(
            -1, -1,
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            helper.ccVariable,
            helper.getCurrentCcCall.type,
            helper.getCurrentCcCall
        )

        // OUTPUT: val __cc = CompositionContext.current
        (compose.body as IrBlockBody).statements.add(0, ccVariableDeclaration)

        val runVariableDeclaration = context.symbolTable.declareVariable(
            -1, -1,
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            helper.runVariable,
            context.builtIns.booleanType.toIrType()!!,
            IrConstImpl.boolean(-1, -1, context.irBuiltIns.booleanType, false)
        )

        // OUTPUT: var __run = false
        (compose.body as IrBlockBody).statements.add(0, runVariableDeclaration)

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

private val KTX_TAG_ORIGIN = object : IrStatementOriginImpl("KTX Tag") {}

private class ComposeFunctionHelper(val context: GeneratorContext, val compose: IrFunction) {
    // make a local unique index generator for tmp var creation and slotting
    val uniqueIndex = run { var i = 0; { i++ } }

    val functionDescription by lazy {
        compose.descriptor.fqNameSafe.asString()
    }

    val ccClass by lazy {
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(R4aUtils.r4aFqName("CompositionContext")))!!
    }

    val componentClass by lazy {
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(R4aUtils.r4aFqName("Component")))!!
    }

    val getCurrentCcFunction by lazy {
        ccClass.companionObjectDescriptor!!.unsubstitutedMemberScope.getContributedVariables(
            Name.identifier("current"),
            NoLookupLocation.FROM_BACKEND
        ).single().getter!!
    }

    val getCurrentCcCall by lazy {
        IrGetterCallImpl(
            -1, -1,
            getCurrentCcFunction.returnType!!.toIrType()!!,
            context.symbolTable.referenceSimpleFunction(getCurrentCcFunction),
            getCurrentCcFunction,
            0
        ).apply {
            dispatchReceiver = IrGetObjectValueImpl(
                -1, -1,
                ccClass.companionObjectDescriptor!!.defaultType.toIrType()!!,
                context.symbolTable.referenceClass(ccClass.companionObjectDescriptor!!)
            )
        }
    }

    val ccVariable by lazy {
        IrTemporaryVariableDescriptorImpl(compose.descriptor, Name.identifier("__cc"), ccClass.defaultType, false)
    }

    val getCc by lazy {
        IrGetValueImpl(-1, -1, context.symbolTable.referenceVariable(ccVariable))
    }

    val runVariable by lazy {
        IrTemporaryVariableDescriptorImpl(compose.descriptor, Name.identifier("__run"), context.builtIns.booleanType, true)
    }

    val getRun by lazy {
        IrGetValueImpl(-1, -1, context.symbolTable.referenceVariable(runVariable))
    }

    fun setRun(value: Boolean): IrSetVariable {
        return IrSetVariableImpl(
            -1,
            -1,
            runVariable.type.toIrType()!!,
            context.symbolTable.referenceVariable(runVariable),
            IrConstImpl(
                -1,
                -1,
                context.irBuiltIns.booleanType,
                IrConstKind.Boolean,
                value
            ),
            KTX_TAG_ORIGIN
        )
    }

    val ccStartViewWithKey by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("startView"),
            NoLookupLocation.FROM_BACKEND
        ).find { it.valueParameters.size == 2 }!!
    }

    val ccStartViewWithoutKey by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("startView"),
            NoLookupLocation.FROM_BACKEND
        ).find { it.valueParameters.size == 1 }!!
    }

    val ccStartMethodWithKey by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("start"),
            NoLookupLocation.FROM_BACKEND
        ).find { it.valueParameters.size == 2 }!!
    }

    val ccJoinKeyMethod by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("joinKey"),
            NoLookupLocation.FROM_BACKEND
        ).single()
    }

    val ccStartMethodWithoutKey by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("start"),
            NoLookupLocation.FROM_BACKEND
        ).find { it.valueParameters.size == 1 }!!
    }

    val androidContextGetter by lazy {
        ccClass.unsubstitutedMemberScope.getContributedVariables(
            Name.identifier("context"),
            NoLookupLocation.FROM_BACKEND
        ).single().getter!!
    }

    val getAndroidContextCall by lazy {
        IrCallImpl(
            -1,
            -1,
            androidContextGetter.returnType!!.toIrType()!!,
            context.symbolTable.referenceFunction(androidContextGetter)
        ).apply {
            dispatchReceiver = getCc
        }
    }

    val isInsertingCall by lazy {
        IrCallImpl(
            -1, -1,
            ccIsInsertingMethod.returnType!!.toIrType()!!,
            context.symbolTable.referenceFunction(ccIsInsertingMethod)
        ).apply {
            dispatchReceiver = getCc
        }
    }

    fun getUseInstanceCall(type: KotlinType): IrExpression {
        return IrTypeOperatorCallImpl(
            -1, -1,
            context.builtIns.nullableAnyType.toIrType()!!,
            IrTypeOperator.SAFE_CAST,
            type.toIrType()!!,
            context.symbolTable.referenceClassifier(type.constructor.declarationDescriptor!!),
            IrCallImpl(
                -1, -1,
                ccUseInstanceMethod.returnType!!.toIrType()!!,
                context.symbolTable.referenceFunction(ccUseInstanceMethod)
            ).apply {
                dispatchReceiver = getCc
            }
        )
    }

    val ccSetInstanceFunctionDescriptor by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("setInstance"),
            NoLookupLocation.FROM_BACKEND
        ).single() // only one of these for now
    }

    val ccUpdateAttributeFunctionDescriptor by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("updateAttribute"),
            NoLookupLocation.FROM_BACKEND
        ).single()
    }

    val ccAttributeChangedFunctionDescriptor by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("attributeChanged"),
            NoLookupLocation.FROM_BACKEND
        ).single()
    }

    val ccAttributeChangedOrInsertingFunctionDescriptor by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("attributeChangedOrInserting"),
            NoLookupLocation.FROM_BACKEND
        ).single()
    }

    val ccComposeMethod by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("compose"),
            NoLookupLocation.FROM_BACKEND
        ).single() // only one of these for now
    }

    val componentComposeMethod by lazy {
        componentClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("compose"),
            NoLookupLocation.FROM_BACKEND
        ).single() // only one of these for now
    }

    val ccIsInsertingMethod by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("isInserting"),
            NoLookupLocation.FROM_BACKEND
        ).single() // only one of these for now
    }

    val ccUseInstanceMethod by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("useInstance"),
            NoLookupLocation.FROM_BACKEND
        ).single() // only one of these for now
    }

    val ccWillComposeMethod by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("willCompose"),
            NoLookupLocation.FROM_BACKEND
        ).single() // only one of these for now
    }

    val ccWillComposeMethodCall by lazy {
        IrCallImpl(-1, -1, ccWillComposeMethod.returnType!!.toIrType()!!, context.symbolTable.referenceFunction(ccWillComposeMethod)).apply {
            dispatchReceiver = getCc
        }
    }

    val ccEndMethod = ccClass.unsubstitutedMemberScope.getContributedFunctions(
        Name.identifier("end"),
        NoLookupLocation.FROM_BACKEND
    ).single()

    val ccEndMethodCall by lazy {
        IrCallImpl(-1, -1, ccEndMethod.returnType!!.toIrType()!!, context.symbolTable.referenceFunction(ccEndMethod)).apply {
            dispatchReceiver = getCc
        }
    }
}
