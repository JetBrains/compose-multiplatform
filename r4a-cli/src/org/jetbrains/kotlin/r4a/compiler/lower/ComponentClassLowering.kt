package org.jetbrains.kotlin.r4a.compiler.lower

import org.jetbrains.kotlin.backend.jvm.codegen.IrExpressionLambda
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxTag
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.analysis.ComposableType
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.KotlinType
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
    tag: IrKtxTag,
    helper: ComposeFunctionHelper
): List<IrStatement> {
    val slotIndex = helper.uniqueIndex()
    val output: MutableList<IrStatement> = mutableListOf()

    val element = tag.element // TODO(jim): Should transform pure IR rather than relying on the element
    val tagNameElement = element.simpleTagName ?: element.qualifiedTagName ?: throw NullPointerException("tag name not found")

    val tagDescriptor = context.bindingContext.get(
        R4AWritableSlices.KTX_TAG_TYPE_DESCRIPTOR,
        element
    ) ?: throw Exception("no tag descriptor found") // as ClassifierDescriptor? ?: throw NullPointerException("KTX tag does not know descriptor: " + element.text)

    val tagType = context.bindingContext.get(
        R4AWritableSlices.KTX_TAG_INSTANCE_TYPE,
        element
    ) ?: throw Exception("no tag type found")

    val composableType = context.bindingContext.get(
        R4AWritableSlices.KTX_TAG_COMPOSABLE_TYPE,
        element
    ) ?: ComposableType.UNKNOWN


    val keyAttribute = tag.attributes.find { it.element.key?.getReferencedName() == "key" }
    val parameterSize = if (keyAttribute != null) 2 else 1
    val ccStartMethod = if (keyAttribute != null) helper.ccStartMethodWithKey else helper.ccStartMethodWithoutKey

    val ccStartMethodCall = IrCallImpl(
        element.startOffset, element.endOffset,
        context.symbolTable.referenceFunction(ccStartMethod)
    ).apply {
        dispatchReceiver = helper.getCc
        putValueArgument(
            0,
            IrConstImpl.int(
                element.startOffset,
                element.endOffset,
                context.builtIns.intType,
                md5IntHash("${helper.functionDescription}::$slotIndex")
            )
        )
        if (keyAttribute != null) {
            putValueArgument(1, keyAttribute.value!!)
        }
    }
    val nullableTagType = tagType.makeNullable()

    val elVariable = IrTemporaryVariableDescriptorImpl(
        helper.compose.descriptor,
        Name.identifier("__el$slotIndex"),
        nullableTagType,
        false
    )

    val classifier = tagDescriptor as? ClassifierDescriptor
            ?: tagType.constructor.declarationDescriptor
            ?: throw Exception("couldnt get type")

    val getNullableInstanceStartCall = IrTypeOperatorCallImpl(
        element.startOffset, element.endOffset,
        context.builtIns.nullableAnyType,
        IrTypeOperator.SAFE_CAST,
        tagType,
        ccStartMethodCall,
        context.symbolTable.referenceClassifier(classifier)
    )

    val elVariableDeclaration = context.symbolTable.declareVariable(
        -1, -1,
        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        elVariable,
        getNullableInstanceStartCall
    )

    // OUTPUT: var el = cc.start(...) as? TagType
    output.add(elVariableDeclaration)

    val elSymbol = context.symbolTable.referenceVariable(elVariable)
    val getEl = IrGetValueImpl(tagNameElement.startOffset, tagNameElement.endOffset, elSymbol)


    val elIsNullExpr = IrBinaryPrimitiveImpl(
        tagNameElement.startOffset, tagNameElement.endOffset,
        IrStatementOrigin.EQEQ,
        context.irBuiltIns.eqeqSymbol,
        getEl,
        IrConstImpl.constNull(tagNameElement.startOffset, tagNameElement.endOffset, context.builtIns.nullableNothingType)
    )

    val instanceCtorCall = when (composableType) {
        ComposableType.VIEW -> {
            val instanceCtor = (tagDescriptor as ClassDescriptor).constructors.find { it.valueParameters.size == 1 }!!

            IrCallImpl(tagNameElement.startOffset, tagNameElement.endOffset, context.symbolTable.referenceConstructor(instanceCtor)).apply {
                putValueArgument(0, helper.getAndroidContextCall)
            }
        }
        ComposableType.COMPONENT -> {
            val instanceCtor = (tagDescriptor as ClassDescriptor).constructors.find { it.valueParameters.size == 0 }!!
            IrCallImpl(tagNameElement.startOffset, tagNameElement.endOffset, context.symbolTable.referenceConstructor(instanceCtor))
        }
        ComposableType.FUNCTION_VAR -> {
            // we already have the instance... its the open tag itself
            tag.openTagExpr ?: throw Exception("open tag expression expected")
        }
        ComposableType.FUNCTION -> TODO()
        ComposableType.UNKNOWN -> throw Exception("unknown component type found: $composableType")
    }

    val storeInstanceExpr =
        IrSetVariableImpl(tagNameElement.startOffset, tagNameElement.endOffset, elSymbol, instanceCtorCall, KTX_TAG_ORIGIN)

    val callSetInstanceExpr = IrCallImpl(
        tagNameElement.startOffset, tagNameElement.endOffset,
        context.symbolTable.referenceFunction(helper.ccSetInstanceFunctionDescriptor)
    ).apply {
        dispatchReceiver = helper.getCc
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

    for (attr in tag.attributes) {

        // for each attribute, we call into the context to see if we need to call the updater, and call if it we need to do so.

        val attrElement = attr.element

        val key = attrElement.key!!.getReferencedName()

        val attributeType = context.bindingContext.get(
            R4AWritableSlices.KTX_ATTR_TYPE,
            attrElement
        ) ?: throw Exception("type for $key not found")

        // store expression into tmp variable
        val attrVariable = IrTemporaryVariableDescriptorImpl(
            helper.compose.descriptor,
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
            context.symbolTable.referenceFunction(helper.ccUpdAttrFunctionDescriptor)
        ).apply {
            dispatchReceiver = helper.getCc
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

        when (composableType) {
            // NOTE(lmr): right now component/view look the same, but whenever we move off of the main thread
            // they will look different
            ComposableType.COMPONENT, ComposableType.VIEW -> {
                val attributeDescriptor = context.bindingContext.get(
                    R4AWritableSlices.KTX_ATTR_DESCRIPTOR,
                    attrElement
                ) ?: throw Exception("setter $key not found")

                val setterFunctionDescriptor = when (attributeDescriptor) {
                    is FunctionDescriptor -> attributeDescriptor
                    is PropertyDescriptor -> attributeDescriptor.setter!!
                    else -> throw Exception("dont know how to handle setting attr $key")
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
            ComposableType.FUNCTION_VAR -> {
                // OUTPUT: cc.updAttr("someAttribute", _el_attrName)
                output.add(callUpdAttrExpr)
            }
            ComposableType.FUNCTION, ComposableType.UNKNOWN -> TODO()
        }
    }

    if (tag.body != null)
        for (statement in tag.body) {
            if (statement is IrKtxTag) output.addAll(transform(context, statement, helper))
            else output.add(statement)
        }

    when (composableType) {
        ComposableType.COMPONENT, ComposableType.FUNCTION_VAR -> {
            // NOTE(lmr): right now we just call cc.compose() for function var composables. This is because internally we end up
            // using reflection to invoke the function with the arguments passed in. This is less than ideal so will likely diverge
            // in the future, but I could not figure out how to invoke the function locally properly using IR.

            // if the component type is a composite component, we need to call "compose" now to recurse down the tree
            // OUTPUT: cc.compose()
            output.add(helper.ccComposeMethodCall)
        }
        ComposableType.VIEW -> Unit
        ComposableType.UNKNOWN -> TODO()
        ComposableType.FUNCTION -> TODO()
    }

    // OUTPUT: cc.end()
    output.add(helper.ccEndMethodCall)

    return output
}

fun lowerComposeFunction(context: GeneratorContext, compose: IrFunction) {
    context.symbolTable.withScope(compose.descriptor) {

        val helper = ComposeFunctionHelper(context, compose)
        // at the beginning of every compose function, we store CompositionContext.current into a local variable. This is just temporary
        // until we figure out the best way to properly thread context through. This current design is assuming everything is done on the
        // main thread, so we have some wiggle room.

        val ccVariableDeclaration = context.symbolTable.declareVariable(
            -1, -1,
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            helper.ccVariable,
            helper.getCurrentCcCall
        )

        // OUTPUT: val __cc = CompositionContext.current
        (compose.body as IrBlockBody).statements.add(0, ccVariableDeclaration)

        // Transform the KTX tags within compose
        compose.body!!.accept(object : IrElementTransformer<Nothing?> {
            override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?): IrElement {
                val block = IrBlockImpl(
                    expression.startOffset,
                    expression.endOffset,
                    context.moduleDescriptor.builtIns.unitType,
                    KTX_TAG_ORIGIN,
                    transform(context, expression as IrKtxTag, helper)
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

private class ComposeFunctionHelper(val context: GeneratorContext, val compose: IrFunction) {
    // make a local unique index generator for tmp var creation and slotting
    val uniqueIndex = run { var i = 0; { i++ } }

    val functionDescription by lazy {
        compose.descriptor.fqNameSafe.asString()
    }

    val ccClass by lazy {
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(R4aUtils.r4aFqName("CompositionContext")))!!
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
    }

    val ccVariable by lazy {
        IrTemporaryVariableDescriptorImpl(compose.descriptor, Name.identifier("__cc"), ccClass.defaultType, false)
    }

    val getCc by lazy {
        IrGetValueImpl(-1, -1, context.symbolTable.referenceVariable(ccVariable))
    }

    val ccStartMethodWithKey by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("start"),
            NoLookupLocation.FROM_BACKEND
        ).find { it.valueParameters.size == 2 }!!
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

    val getAndroidContextCall by lazy{
        IrCallImpl(
            -1,
            -1,
            context.symbolTable.referenceFunction(androidContextGetter)
        ).apply {
            dispatchReceiver = getCc
        }
    }

    val ccSetInstanceFunctionDescriptor by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("setInstance"),
            NoLookupLocation.FROM_BACKEND
        ).single() // only one of these for now
    }

    val ccUpdAttrFunctionDescriptor by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("updAttr"),
            NoLookupLocation.FROM_BACKEND
        ).single()
    }

    val ccComposeMethod by lazy {
        ccClass.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("compose"),
            NoLookupLocation.FROM_BACKEND
        ).single() // only one of these for now
    }

    val ccComposeMethodCall by lazy {
        IrCallImpl(-1, -1, context.symbolTable.referenceFunction(ccComposeMethod)).apply {
            dispatchReceiver = getCc
        }
    }

    val ccEndMethod = ccClass.unsubstitutedMemberScope.getContributedFunctions(
        Name.identifier("end"),
        NoLookupLocation.FROM_BACKEND
    ).single()

    val ccEndMethodCall by lazy {
        IrCallImpl(-1, -1, context.symbolTable.referenceFunction(ccEndMethod)).apply {
            dispatchReceiver = getCc
        }
    }
}
