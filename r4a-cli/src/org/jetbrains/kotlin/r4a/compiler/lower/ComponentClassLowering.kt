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
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.r4a.R4ASyntheticIrExtension
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxTag
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.r4a.compiler.ir.buildWithScope
import org.jetbrains.kotlin.r4a.compiler.ir.find
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest

/**
 * Creates synthetics and applies KTX lowering on a class-component.
 */
fun lowerComponentClass(context: GeneratorContext, metadata: ComponentMetadata, component: IrClass) {
    // TODO: if (component.annotations.includes(GenerateWrapperViewAnnotation) { ...
    component.declarations.add(generateWrapperView(context, metadata))
    // }

    component.declarations.add(generateComponentCompanionObject(context, metadata))

    lowerComposeFunction(context, component)
}

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
 *     // we generate a `sourceKey` in this transform function which is meant to be a key that is
 *     // unique to the source location of the KTX element.
 *     var tmpEl = cc.start(sourceKey) as? Foo
 *     if (tmpEl == null) {
 *         // if Foo was a View, we would pass in the `cc.context` parameter
 *         tmpEl = Foo()
 *         cc.setInstance(tmpEl)
 *     }
 *
 *     // we then iterate over each attribute, setting only the ones that have changed
 *     val tmpBar = expr1
 *     if (cc.updAttr("bar", tmpBar)) {
 *         tmpEl.setBar(tmpBar)
 *     }
 *     val tmpBam = expr2
 *     if (cc.updAttr("bam", tmpBam)) {
 *         tmpEl.setBar(tmpBam)
 *     }
 *
 *     // if there is a body for this element, it will execute that code as normal, including any
 *     // KTX statements that are a part of it
 *     ...expr3...
 *
 *     // if the element is a component, we recurse down:
 *     cc.compose()
 *
 *     // now we are done with the element
 *     cc.end()
 *
 */
private fun transform(
    context: GeneratorContext,
    component: IrClass,
    function: IrFunction,
    tag: IrKtxTag,
    ccVariable: IrTemporaryVariableDescriptor,
    uniqueIndex: () -> Int
): List<IrStatement> {
    val slotIndex = uniqueIndex()
    val output: MutableList<IrStatement> = mutableListOf()

    val element = tag.element // TODO(jim): Should transform pure IR rather than relying on the element
    val tagNameElement = element.simpleTagName ?: element.qualifiedTagName ?: throw NullPointerException("tag name not found")

    val tagType = context.bindingContext.get(
        R4AWritableSlices.KTX_TAG_TYPE_DESCRIPTOR,
        element
    ) as ClassifierDescriptor? ?: throw NullPointerException("KTX tag does not know descriptor: " + element.text)

    val componentType = context.bindingContext.get(
        R4AWritableSlices.KTX_TAG_COMPONENT_TYPE,
        element
    ) ?: -1

    val ccClass = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(R4aUtils.r4aFqName("CompositionContext")))!!

    val getCc = IrGetValueImpl(element.startOffset, element.endOffset, context.symbolTable.referenceVariable(ccVariable))

    val keyAttribute = tag.attributes.find { it.element.key?.getReferencedName() == "key" }
    val parameterSize = if (keyAttribute != null) 2 else 1
    println("parameterSize: $parameterSize")

    val ccStartMethod = ccClass.unsubstitutedMemberScope.getContributedFunctions(
        Name.identifier("start"),
        NoLookupLocation.FROM_BACKEND
    ).find { it.valueParameters.size == parameterSize }!!

    val ccStartMethodCall = IrCallImpl(
        element.startOffset, element.endOffset,
        context.symbolTable.referenceFunction(ccStartMethod)
    ).apply {
        dispatchReceiver = getCc
        putValueArgument(
            0,
            IrConstImpl.int(
                element.startOffset,
                element.endOffset,
                context.builtIns.intType,
                md5IntHash("${component.descriptor.fqNameSafe.asString()}::$slotIndex")
            )
        )
        if (keyAttribute != null) {
             putValueArgument(1, keyAttribute.value!!)
        }
    }

    val nullableTagType = tagType.defaultType.makeNullable()

    val getNullableInstanceStartCall = IrTypeOperatorCallImpl(
        element.startOffset, element.endOffset,
        context.builtIns.nullableAnyType,
        IrTypeOperator.SAFE_CAST,
        tagType.defaultType,
        ccStartMethodCall,
        context.symbolTable.referenceClassifier(tagType)
    )

    val elVariable = IrTemporaryVariableDescriptorImpl(
        function.descriptor,
        Name.identifier("__el$slotIndex"),
        nullableTagType,
        false
    )

    val elVariableDeclaration = context.symbolTable.declareVariable(
        -1, -1,
        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        elVariable,
        getNullableInstanceStartCall
    )

    val elSymbol = context.symbolTable.referenceVariable(elVariable)
    val getEl = IrGetValueImpl(tagNameElement.startOffset, tagNameElement.endOffset, elSymbol)

    // OUTPUT: var el = cc.start(...) as? TagType
    output.add(elVariableDeclaration)

    val elIsNullExpr = IrBinaryPrimitiveImpl(
        tagNameElement.startOffset, tagNameElement.endOffset,
        IrStatementOrigin.EQEQ,
        context.irBuiltIns.eqeqSymbol,
        getEl,
        IrConstImpl.constNull(tagNameElement.startOffset, tagNameElement.endOffset, context.builtIns.nullableNothingType)
    )

    val instanceCtorCall = when (componentType) {
    // Native View
        0 -> {
            val instanceCtor = (tagType as ClassDescriptor).constructors.find { it.valueParameters.size == 1 }!!

            val contextGetter = ccClass.unsubstitutedMemberScope.getContributedVariables(
                Name.identifier("context"),
                NoLookupLocation.FROM_BACKEND
            ).single().getter!!
            val getContextCall = IrCallImpl(
                tagNameElement.startOffset,
                tagNameElement.endOffset,
                context.symbolTable.referenceFunction(contextGetter)
            ).apply {
                dispatchReceiver = getCc
            }

            IrCallImpl(tagNameElement.startOffset, tagNameElement.endOffset, context.symbolTable.referenceConstructor(instanceCtor)).apply {
                putValueArgument(0, getContextCall)
            }
        }
    // Composite Component
        1 -> {
            val instanceCtor = (tagType as ClassDescriptor).constructors.find { it.valueParameters.size == 0 }!!
            IrCallImpl(tagNameElement.startOffset, tagNameElement.endOffset, context.symbolTable.referenceConstructor(instanceCtor))
        }
        else -> throw Exception("unknown component type found: $componentType")
    }

    val storeInstanceExpr =
        IrSetVariableImpl(tagNameElement.startOffset, tagNameElement.endOffset, elSymbol, instanceCtorCall, KTX_TAG_ORIGIN)

    val ccSetInstanceFunctionDescriptor = ccClass.unsubstitutedMemberScope.getContributedFunctions(
        Name.identifier("setInstance"),
        NoLookupLocation.FROM_BACKEND
    ).single() // only one of these for now

    val callSetInstanceExpr = IrCallImpl(
        tagNameElement.startOffset, tagNameElement.endOffset,
        context.symbolTable.referenceFunction(ccSetInstanceFunctionDescriptor)
    ).apply {
        dispatchReceiver = getCc
        putValueArgument(0, getEl)
    }

    val thenBranchExpr = IrBlockImpl(
        tagNameElement.startOffset, tagNameElement.endOffset,
        context.builtIns.unitType,
        KTX_TAG_ORIGIN,
        listOf(storeInstanceExpr, callSetInstanceExpr)
    )

    val ifNullExpr = IrIfThenElseImpl(
        tagNameElement.startOffset, tagNameElement.endOffset,
        context.builtIns.unitType,
        elIsNullExpr, // condition
        thenBranchExpr
    )

    // OUTPUT:
    // if (el != null) {
    //   el = SomeType(...)
    //   cc.setInstance(el)
    // }
    output.add(ifNullExpr)

    val ccUpdAttrFunctionDescriptor = ccClass.unsubstitutedMemberScope.getContributedFunctions(
        Name.identifier("updAttr"),
        NoLookupLocation.FROM_BACKEND
    ).single()


    for (attr in tag.attributes) {

        // for each attribute, we call into the context to see if we need to call the updater, and call if it we need to do so.

        val attrElement = attr.element

        val key = attrElement.key!!.getReferencedName()

        val attributeDescriptor = context.bindingContext.get(
            R4AWritableSlices.KTX_ATTR_DESCRIPTOR,
            attrElement
        ) ?: throw Exception("setter $key not found")

        val setterFunctionDescriptor = when (attributeDescriptor) {
            is FunctionDescriptor -> attributeDescriptor
            is PropertyDescriptor -> attributeDescriptor.setter!!
            else -> throw Exception("dont know how to handle setting attr $key")
        }

        val attributeType = context.bindingContext.get(
            R4AWritableSlices.KTX_ATTR_TYPE,
            attrElement
        ) ?: throw Exception("type for $key not found")

        // store expression into tmp variable
        val attrVariable = IrTemporaryVariableDescriptorImpl(
            function.descriptor,
            Name.identifier("__el${slotIndex}_$key"),
            attributeType,
            false
        )

        val attrVariableDeclaration = context.symbolTable.declareVariable(
            attrElement.startOffset, attrElement.endOffset,
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            attrVariable,
            attr.value!!
        )

        // OUTPUT: val _el_attrName = (...attrExpression...)
        output.add(attrVariableDeclaration)

        val getAttr = IrGetValueImpl(attrElement.startOffset, attrElement.endOffset, context.symbolTable.referenceVariable(attrVariable))

        val callUpdAttrExpr = IrCallImpl(
            attrElement.startOffset, attrElement.endOffset,
            context.symbolTable.referenceFunction(ccUpdAttrFunctionDescriptor)
        ).apply {
            dispatchReceiver = getCc
            putValueArgument(
                0,
                IrConstImpl.string(
                    attrElement.key!!.startOffset,
                    attrElement.key!!.endOffset,
                    context.builtIns.stringType,
                    key
                )
            )
            putValueArgument(1, getAttr)
        }

        val thenUpdBranchExpr = IrCallImpl(
            attrElement.startOffset, attrElement.endOffset,
            context.symbolTable.referenceFunction(setterFunctionDescriptor)
        )

        when (setterFunctionDescriptor.valueParameters.size) {
            1 -> {
                // if single parameter, its a setter function and the element is the receiver
                thenUpdBranchExpr.dispatchReceiver = getEl
                thenUpdBranchExpr.putValueArgument(0, getAttr)
            }
            2 -> {
                // if there are two parameters, its an "adapter" setter function, in which case the element
                // is the first argument, the attribute value is the second, and the receiver is some static class
                // instance.
                val staticInstDescriptor = setterFunctionDescriptor.containingDeclaration as ClassDescriptor
                thenUpdBranchExpr.dispatchReceiver = IrGetObjectValueImpl(
                    attrElement.startOffset, attrElement.endOffset,
                    staticInstDescriptor.defaultType,
                    context.symbolTable.referenceClass(staticInstDescriptor)
                )
                thenUpdBranchExpr.putValueArgument(0, getEl)
                thenUpdBranchExpr.putValueArgument(1, getAttr)
            }
        }

        val updateIfNeededExpr = IrIfThenElseImpl(
            attrElement.startOffset, attrElement.endOffset,
            context.builtIns.unitType,
            callUpdAttrExpr, // condition
            thenUpdBranchExpr
        )

        // OUTPUT:
        // if (cc.updAttr("someAttribute", _el_attrName)) {
        //   el.setSomeAttribute(_el_attrName)
        // }
        output.add(updateIfNeededExpr)
    }

    if (tag.body != null)
        for (statement in tag.body) {
            if (statement is IrKtxTag) output.addAll(transform(context, component, function, statement, ccVariable, uniqueIndex))
            else output.add(statement)
        }

    when (componentType) {
        1 -> {
            // if the component type is a composite component, we need to call "compose" now to recurse down the tree
            val ccComposeMethod = ccClass.unsubstitutedMemberScope.getContributedFunctions(
                Name.identifier("compose"),
                NoLookupLocation.FROM_BACKEND
            ).single() // only one of these for now

            val ccComposeMethodCall =
                IrCallImpl(element.startOffset, element.endOffset, context.symbolTable.referenceFunction(ccComposeMethod))
            ccComposeMethodCall.dispatchReceiver = getCc

            // TODO(lmr): eventually, we can prune this call if none of the attributes have updated. We can't at the moment since every
            // "recompose" is done from the root, and private state changes won't be reflected. To fix, we need to figure out how to
            // mark a subtree as dirty, and recompose only that subtree.

            // OUTPUT: cc.compose()
            output.add(ccComposeMethodCall)
        }
    }

    val ccEndMethod = ccClass.unsubstitutedMemberScope.getContributedFunctions(
        Name.identifier("end"),
        NoLookupLocation.FROM_BACKEND
    ).single()

    val ccEndMethodCall = IrCallImpl(element.startOffset, element.endOffset, context.symbolTable.referenceFunction(ccEndMethod))
    ccEndMethodCall.dispatchReceiver = getCc

    // OUTPUT: cc.end()
    output.add(ccEndMethodCall)

    return output
}

private fun lowerComposeFunction(context: GeneratorContext, component: IrClass) {
    val compose = component.declarations
        .mapNotNull { it as? IrSimpleFunction }
        .find { it.name.toString() == "compose" && it.descriptor.valueParameters.size == 0 } ?: return

    // make a local unique index generator for tmp var creation and slotting
    val uniqueIndex = run { var i = 0; { i++ } }

    // TODO(lmr): try using this in order to utilize the builder DSL
//        val builder = context.createIrBuilder(newIrFunction.symbol)
//        val body = builder.irBlockBody(irFunction) {

    context.symbolTable.withScope(compose.descriptor) {

        // at the beginning of every compose function, we store CompositionContext.current into a local variable. This is just temporary
        // until we figure out the best way to properly thread context through. This current design is assuming everything is done on the
        // main thread, so we have some wiggle room.

        val ccClass =
            context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(R4aUtils.r4aFqName("CompositionContext")))!!

        val getCurrentCcFunction = ccClass.companionObjectDescriptor!!.unsubstitutedMemberScope.getContributedVariables(
            Name.identifier("current"),
            NoLookupLocation.FROM_BACKEND
        ).single().getter!!

        val getCurrentCcCall = IrGetterCallImpl(
            -1, -1,
            context.symbolTable.referenceSimpleFunction(getCurrentCcFunction),
            getCurrentCcFunction,
            0
        ).apply {
            dispatchReceiver = IrGetObjectValueImpl(
                -1, -1,
                ccClass.companionObjectDescriptor!!.defaultType,
                context.symbolTable.referenceClass(ccClass.companionObjectDescriptor!!)
            )
        }

        val ccVariable = IrTemporaryVariableDescriptorImpl(compose.descriptor, Name.identifier("__cc"), ccClass.defaultType, false)

        val ccVariableDeclaration = context.symbolTable.declareVariable(
            -1, -1,
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            ccVariable,
            getCurrentCcCall
        )

        // OUTPUT: val __cc = CompositionContext.current
        (compose.body as IrBlockBody).statements.add(0, ccVariableDeclaration)

        // Transform the KTX tags within compose
        compose.body!!.accept(object : IrElementTransformer<Nothing?> {
            override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?): IrElement {
                val block = IrBlockImpl(
                    -1,
                    -1,
                    context.moduleDescriptor.builtIns.unitType,
                    KTX_TAG_ORIGIN,
                    transform(context, component, compose, expression as IrKtxTag, ccVariable, uniqueIndex)
                )
                block.accept(this, data)
                return block
            }
        }, null)

    }
}

private val KTX_TAG_ORIGIN = object : IrStatementOriginImpl("KTX Tag") {}

private fun md5IntHash(string: String): Int {
    val md = MessageDigest.getInstance("MD5")
    md.update(string.toByteArray(Charset.defaultCharset()))
    return ByteBuffer.wrap(md.digest()).int
}
