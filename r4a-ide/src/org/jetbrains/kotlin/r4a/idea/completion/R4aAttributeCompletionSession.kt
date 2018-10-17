package org.jetbrains.kotlin.r4a.idea.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.JBColor
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.completion.CompletionSessionConfiguration
import org.jetbrains.kotlin.idea.completion.ToFromOriginalFileMapper
import org.jetbrains.kotlin.idea.completion.isArtificialImportAliasedDescriptor
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.inspections.collections.isFunctionOfAnyKind
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.KtxLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.r4a.idea.parentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.supertypes

class R4aAttributeCompletionSession(
    configuration: CompletionSessionConfiguration,
    parameters: CompletionParameters,
    toFromOriginalFileMapper: ToFromOriginalFileMapper,
    resultSet: CompletionResultSet
) : BaseR4aCompletionSession(configuration, parameters, toFromOriginalFileMapper, resultSet) {

    private val elementExpr = cursorElement.parentOfType<KtxElement>() ?: error("no ktx element found")
    private val usedAttributes = elementExpr.getChildrenOfType<KtxAttribute>()
    private val hasChildrenLambda = elementExpr.getChildrenOfType<KtxLambdaExpression>().isNotEmpty()
    private val usedAttributesNameSet = usedAttributes.mapNotNull { attr -> attr.key?.getIdentifier()?.text }.toSet()

    private val nullableTagInfo = bindingContext.get(R4AWritableSlices.KTX_TAG_INFO, elementExpr)

    fun isValid() = nullableTagInfo != null

    private val tagInfo get() = nullableTagInfo ?: error("no tag info found on element. Call isValid() before using this class")


    private fun DeclarationDescriptor.isValidAttribute(): Boolean {
        return when (this) {
            is FunctionDescriptor -> {
                when {
                    valueParameters.size != 1 -> false
                    returnType?.isUnit() != true -> false
                    // only void setters are allowed
                    else -> R4aUtils.isSetterMethodName(name.identifier)
                }
            }
            is PropertyDescriptor -> isVar
            else -> false
        }
    }

    private fun AttributeInfo.constructLookupElement(): LookupElement? {
        val extensionType = extensionType
        val contributingDescriptor = contributingDescriptor

        if (usedAttributesNameSet.contains(name)) return null

        // if the user has defined a chilren lambda already, there is no need to add a children attribute to the
        // autocomplete list
        if (hasChildrenLambda && isChildren) return null

        // NOTE(lmr): if the attribute is a setter function, the parameter name can often be useful. we should consider using its
        // name somewhere in here: (attr.descriptor as SimpleFunctionDescriptor).valueParameters.single().name

        var isImmediate = isImmediate

        var myTailText = "=..."

        if (isExtension && extensionType != null) {
            if (extensionType == tagInfo.instanceType) {
                isImmediate = true
            }
            val klass = SHORT_NAMES_RENDERER.renderType(extensionType)
            myTailText += " (extension on $klass)"
        }

        if (!isImmediate && !isExtension && contributingDescriptor != null) {
            val klass = SHORT_NAMES_RENDERER.renderClassifierName(contributingDescriptor)
            myTailText += " (from $klass)"
        }

        if (isChildren) {
            myTailText += " (@Children)"
        }
        if (isRequired) {
            myTailText += " (required)"
        }
        if (isPivotal) {
            myTailText += " (pivotal)"
        }

        val renderer = object : LookupElementRenderer<LookupElement>() {
            override fun renderElement(element: LookupElement?, presentation: LookupElementPresentation?) {
                if (presentation == null) return
                with(presentation) {
                    itemText = name
                    isItemTextBold = when {
                        isImmediate || isRequired -> true
                        else -> false
                    }
                    itemTextForeground = when {
                        isPivotal -> JBColor.RED
                        isRequired -> JBColor.BLUE
                        isImmediate -> JBColor.BLACK
                        else -> JBColor.darkGray
                    }
                    typeText = SHORT_NAMES_RENDERER.renderType(type)
                    if (isExtension) {
                        appendTailTextItalic(myTailText, true)
                    } else {
                        appendTailText(myTailText, true)
                    }
                }
            }
        }

        return LookupElementBuilder
            .create(this, name)
            .withRenderer(renderer)
            .withInsertHandler(attributeInsertionHandler)
            .apply {
                putUserData(DESCRIPTOR, descriptor)
                putUserData(Weighers.ATTRIBUTE_INFO_KEY, this@constructLookupElement)
            }
    }

    private val kotlinxAndroidSyntheticFqname = FqName("kotlinx.android.synthetic")

    private fun shouldFilter(d: DeclarationDescriptor): Boolean {
        if (d.fqNameSafe.isChildOf(kotlinxAndroidSyntheticFqname)) return true
        val name = d.name.asString()

        when (name) {
            "setRecompose" -> {
                if (d is CallableMemberDescriptor) {
                    val realDescriptor = getContributingDescriptor(d)
                    if (realDescriptor != null && realDescriptor != d && realDescriptor == r4aComponentDescriptor) {
                        return true
                    }
                }
            }
        }

        // NOTE(lmr): View subclasses all have constructors with these params. We handle view creation somewhat automatically, and
        // we don't want these params to show up in the autocomplete despite people technically being able to use them. This is
        // kind of a gross way of removing them but seems to be a reasonably effective strategy.
        when (name) {
            "context",
            "attrs",
            "defStyle",
            "defStyleRes",
            "defStyleAttr" -> {
                if (
                    d is ValueParameterDescriptor &&
                    d.containingDeclaration is ConstructorDescriptor
                ) {
                    val cd = d.containingDeclaration.containingDeclaration as? ClassDescriptor
                    if (
                        cd != null &&
                        androidViewDescriptor != null &&
                        cd.isSubclassOf(androidViewDescriptor)
                    ) {
                        return true
                    }
                }

            }
        }

        return false
    }

    private fun DeclarationDescriptor.constructAttributeInfo(isImported: Boolean): AttributeInfo? {
        if (shouldFilter(this)) return null

        return AttributeInfo(
            name = when (this) {
                is FunctionDescriptor -> R4aUtils.propertyNameFromSetterMethod(name.asString())
                else -> name.asString()
            },
            type = when (this) {
                is FunctionDescriptor -> valueParameters.firstOrNull()?.type ?: return null
                is PropertyDescriptor -> type
                is ValueParameterDescriptor -> type
                else -> return null
            }
        ).also {
            it.descriptor = this
            it.isChildren = hasChildrenAnnotation()
            it.isRequired = when (this) {
                is PropertyDescriptor -> {
                    isLateInit || !isVar
                }
                is FunctionDescriptor -> false
                is ValueParameterDescriptor -> {
                    !hasDefaultValue()
                }
                else -> false
            }
            it.isExtension = isExtension
            it.isImmediate = when {
                this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> false
                containingDeclaration == tagInfo.referrableDescriptor -> true
                containingDeclaration is ConstructorDescriptor && containingDeclaration?.containingDeclaration == tagInfo.referrableDescriptor -> true
                else -> false
            }
            it.isImported = isImported
            it.isPivotal = when (this) {
                is PropertyDescriptor -> !isVar && !isExtension
                is ValueParameterDescriptor -> {
                    if (tagInfo.isConstructed && containingDeclaration is ConstructorDescriptor) {
                        val cd = containingDeclaration.containingDeclaration as ClassDescriptor
                        val propName = name
                        val desc = cd.unsubstitutedMemberScope.getContributedDescriptors()
                            .mapNotNull { it as? PropertyDescriptor }
                            .firstOrNull { it.name == propName }
                        if (desc != null) !desc.isVar // if it's a "val", it's pivotal
                        else true // if desc is null, it's a non-property constructor param, which IS pivotal
                    } else {
                        false // in this case there is no component instance, so nothing is pivotal
                    }
                }
                else -> false
            }
            it.contributingDescriptor = when (this) {
                is FunctionDescriptor -> getContributingDescriptor(this)
                is PropertyDescriptor -> getContributingDescriptor(this)
                else -> null
            }
            it.extensionType = when (this) {
                is FunctionDescriptor -> getExtensionType(this)
                is PropertyDescriptor -> getExtensionType(this)
                else -> null
            }
        }
    }

    private fun getContributingDescriptor(d: CallableMemberDescriptor): ClassifierDescriptor? {
        return DescriptorUtils.unwrapFakeOverride(d).containingDeclaration as? ClassifierDescriptor
    }

    private fun getExtensionType(d: CallableDescriptor): KotlinType? {
        val receiverParameter = d.extensionReceiverParameter ?: return null

        if (receiverParameter.value.original.type.isTypeParameter()) {
            return receiverParameter.value.original.type.supertypes().firstOrNull()
        }
        return receiverParameter.type
    }

    private fun TypeProjection.constructAttributeInfo(): AttributeInfo? {
        return AttributeInfo(
            name = type.extractParameterNameFromFunctionTypeArgument()?.asString() ?: return null,
            type = type
        ).also {
            it.descriptor = null
            it.isChildren = type.hasChildrenAnnotation()
            it.isRequired = true
            it.isExtension = false
            it.isImmediate = true
            it.isImported = true
            it.isPivotal = false
            it.contributingDescriptor = null
            it.extensionType = null
        }
    }

    override fun doComplete() {
        val tagDescriptor = tagInfo.referrableDescriptor
        when (tagDescriptor) {
            is ClassDescriptor -> {
                collector.addElements(
                    tagDescriptor
                        .constructors
                        .filter { it.isVisibleDescriptor() }
                        .flatMap { it.valueParameters }
                        .asSequence()
                        .mapNotNull { it.constructAttributeInfo(isImported = true) }
                        .mapNotNull { it.constructLookupElement() }
                        .asIterable()
                )
                flushToResultSet()

                collector.addElements(
                    tagDescriptor
                        .unsubstitutedMemberScope
                        .getContributedDescriptors()
                        .filter { it.isValidAttribute() }
                        .filter { it.isVisibleDescriptor() }
                        .mapNotNull { it.constructAttributeInfo(isImported = true) }
                        .mapNotNull { it.constructLookupElement() }
                )
                flushToResultSet()

                val indiceshelper = KotlinIndicesHelper(
                    resolutionFacade,
                    searchScope,
                    { it.isVisibleDescriptor() },
                    filterOutPrivate = true,
                    declarationTranslator = { toFromOriginalFileMapper.toSyntheticFile(it) },
                    file = file
                )

                collector.addElements(
                    indiceshelper
                        .getCallableTopLevelExtensions(
                            // NOTE(lmr): cursorElement isn't actually the receiver in this context, but all this function inspects
                            // is that its DOT, so any KtExpression will suffice
                            callTypeAndReceiver = CallTypeAndReceiver.DOT(cursorElement),
                            receiverTypes = listOf(tagDescriptor.defaultType),
                            nameFilter = { true },
                            declarationFilter = { true }
                        )
                        .filter { it.isValidAttribute() }
                        .mapNotNull { it.constructAttributeInfo(isImported = true) }
                        .mapNotNull { it.constructLookupElement() }
                )
                flushToResultSet()
            }
            is FunctionDescriptor -> {
                collector.addElements(
                    tagDescriptor
                        .valueParameters
                        .asSequence()
                        .mapNotNull { it.constructAttributeInfo(isImported = true) }
                        .mapNotNull { it.constructLookupElement() }
                        .asIterable()
                )
                flushToResultSet()
            }
            is VariableDescriptor -> {
                if (tagDescriptor.type.isFunctionOfAnyKind()) {
                    collector.addElements(
                        tagDescriptor.type
                            .getValueParameterTypesFromFunctionType()
                            .asSequence()
                            .mapNotNull { it.constructAttributeInfo() }
                            .mapNotNull { it.constructLookupElement() }
                            .asIterable()
                    )
                    flushToResultSet()
                }

            }
        }
    }

    override fun createSorter(): CompletionSorter {
        return super.createSorter()
            .weighAfter(
                "prefix",
                Weighers.IsRequiredAttribute,
                Weighers.IsImmediateAttribute,
                Weighers.IsImported,
                Weighers.IsChildAttribute
            )
    }

    private val attributeInsertionHandler = InsertHandler<LookupElement> { context, item ->
        val document = context.document
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitAllDocuments()
        item.getUserData(DESCRIPTOR)?.let { descriptor ->
            if (DescriptorUtils.isTopLevelDeclaration(descriptor) && !descriptor.isArtificialImportAliasedDescriptor) {
                ImportInsertHelper.getInstance(context.project)
                    .importDescriptor(context.file as KtFile, descriptor)
            }
        }
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

        val token = context.file.findElementAt(context.startOffset) ?: return@InsertHandler
        val ktxAttribute = token.parent.parent as? KtxAttribute ?: return@InsertHandler

        // If the attribute has an equals sign already, then we don't need to insert it...
        if (ktxAttribute.node.findChildByType(KtTokens.EQ) != null) return@InsertHandler

        // NOTE: If we introduce attribute "punning", we will need to do the right thing here...

        val tailOffset = context.tailOffset
        val moveCaret = context.editor.caretModel.offset == tailOffset
        val textToInsert = "="
        document.insertString(tailOffset, textToInsert)

        if (moveCaret) {
            // move after the '='
            context.editor.caretModel.moveToOffset(tailOffset + 1)

            // since they just created a new attribute and need to fill out the expression, we might as well open up the autocomplete!
            AutoPopupController.getInstance(context.project)?.scheduleAutoPopup(context.editor)
        }
    }

    companion object {
        val DESCRIPTOR = Key<DeclarationDescriptor>("r4a.descriptor")
        private val SHORT_NAMES_RENDERER = DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions { parameterNamesInFunctionalTypes = false }
    }
}

