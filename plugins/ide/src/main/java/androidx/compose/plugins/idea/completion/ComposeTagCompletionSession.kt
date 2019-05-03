package androidx.compose.plugins.idea.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.AllClassesGetter
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiWhiteSpace
import com.intellij.ui.JBColor
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.completion.CompletionSessionConfiguration
import org.jetbrains.kotlin.idea.completion.KindWeigher
import org.jetbrains.kotlin.idea.completion.ToFromOriginalFileMapper
import org.jetbrains.kotlin.idea.completion.asNameFilter
import org.jetbrains.kotlin.idea.completion.isArtificialImportAliasedDescriptor
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.refactoring.hasIdentifiersOnly
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.idea.parentOfType
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.scopes.utils.collectAllFromMeAndParent
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions.INVOKE

class ComposeTagCompletionSession(
    configuration: CompletionSessionConfiguration,
    parameters: CompletionParameters,
    toFromOriginalFileMapper: ToFromOriginalFileMapper,
    resultSet: CompletionResultSet
) : BaseComposeCompletionSession(configuration, parameters, toFromOriginalFileMapper, resultSet) {

    private val elementExpr = cursorElement.parentOfType<KtxElement>()
        ?: error("no ktx element found")
    private val nullableKtxCall =
        bindingContext.get(ComposeWritableSlices.RESOLVED_KTX_CALL, elementExpr)

    private val emitUpperBounds by lazy {
        if (nullableKtxCall == null || nullableKtxCall.getComposerCall == null)
            listOf(DefaultBuiltIns.Instance.any)
        else nullableKtxCall.emitSimpleUpperBoundTypes
            .mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }
    }

    private val emitUpperBoundsPsi by lazy {
        emitUpperBounds.mapNotNull { psiFacade.findClass(it.fqNameSafe.asString(), searchScope) }
    }

    private val indiceshelper = KotlinIndicesHelper(
        resolutionFacade,
        searchScope,
        { true },
        filterOutPrivate = true,
        declarationTranslator = { toFromOriginalFileMapper.toSyntheticFile(it) },
        file = file
    )

    private fun PsiClass.isEmittable(): Boolean {
        return when {
            isSyntheticKotlinClass() -> false
            isAnnotationType -> false
            isInterface -> false
            isEnum -> false
            hasModifier(JvmModifier.ABSTRACT) -> false
            emitUpperBoundsPsi.any { isSubclassOf(it) } -> true
            else -> false
        }
    }

    private fun PsiClass.isSyntheticKotlinClass(): Boolean {
        if ('$' !in name!!) return false // optimization to not analyze annotations of all classes
        val metadata = modifierList?.findAnnotation(JvmAnnotationNames.METADATA_FQ_NAME.asString())
        return (metadata?.findAttributeValue(JvmAnnotationNames.KIND_FIELD_NAME) as?
                PsiLiteral)?.value == KotlinClassHeader.Kind.SYNTHETIC_CLASS.id
    }

    private fun DeclarationDescriptor.childrenParameterNames(): List<String> {
        when (this) {
            is ClassDescriptor -> {
                val descriptor =
                    unsubstitutedMemberScope.getContributedDescriptors().firstOrNull {
                        it.hasChildrenAnnotation()
                    } ?: unsubstitutedPrimaryConstructor?.valueParameters?.firstOrNull {
                        it.hasChildrenAnnotation()
                    } ?: return emptyList()

                return when (descriptor) {
                    is ParameterDescriptor -> descriptor.type.functionParameterNames()
                    is PropertyDescriptor -> descriptor.type.functionParameterNames()
                    is FunctionDescriptor ->
                        descriptor.valueParameters.firstOrNull()?.type?.functionParameterNames()
                            ?: emptyList()
                    else -> emptyList()
                }
            }
            is FunctionDescriptor -> {
                val descriptor = valueParameters.firstOrNull { it.hasChildrenAnnotation() }
                    ?: return emptyList()
                return descriptor.type.functionParameterNames()
            }
            is VariableDescriptor -> {
                val childTypeArg = type.arguments.firstOrNull { it.type.hasChildrenAnnotation() }
                    ?: return emptyList()
                return childTypeArg.type.functionParameterNames()
            }
        }

        return emptyList()
    }

    private fun DeclarationDescriptor.constructLookupElement(): LookupElement? {
        val isVisible = isVisibleDescriptor()
        val parentFqName = fqNameSafe.parent()

        val fileFqName = file.packageFqName

        val packageText = if (fileFqName != parentFqName) parentFqName.render() else ""

        val tailText = if (packageText.isNotBlank()) " ($packageText)" else ""

        val allowsChildren = allowsChildren()
        val parameterNames = if (allowsChildren) childrenParameterNames() else emptyList()

        val presentableText =
            if (allowsChildren && parameterNames.isNotEmpty())
                "<${name.asString()}> ${parameterNames.joinToString()} -> ... </${name.asString()}>"
            else if (allowsChildren)
                "<${name.asString()}>...</${name.asString()}>"
            else
                "<${name.asString()} />"

        val descriptor = this
        val psiDecl = when (descriptor) {
            is ConstructorDescriptor -> descriptor.containingDeclaration
            else -> descriptor
        }

        val renderer = object : LookupElementRenderer<LookupElement>() {
            override fun renderElement(
                element: LookupElement?,
                presentation: LookupElementPresentation?
            ) {
                if (presentation == null) return
                with(presentation) {
                    val psiElement = DescriptorToSourceUtilsIde.getAnyDeclaration(project, psiDecl)
                    itemText = presentableText
                    isItemTextBold = isVisible
                    icon = KotlinDescriptorIconProvider.getIcon(
                        descriptor,
                        psiElement,
                        Iconable.ICON_FLAG_VISIBILITY
                    )
                    appendTailText(tailText, true)
                }
            }
        }

        return LookupElementBuilder.create(this, name.asString())
            .withRenderer(renderer)
            .withInsertHandler(tagInsertionHandler)
            .apply {
                putUserData(ALLOWS_CHILDREN, allowsChildren)
                putUserData(DESCRIPTOR, this@constructLookupElement)
                putUserData(Weighers.IS_IMPORTED_KEY, isVisible)
            }
    }

    private fun PsiClass.constructLookupElement(): LookupElement? {
        val parentFqName = getKotlinFqName()?.parent() ?: return null

        val name = name ?: return null

        val fileFqName = file.packageFqName

        val packageText = if (fileFqName != parentFqName) parentFqName.render() else ""

        val tailText = if (packageText.isNotBlank()) " ($packageText)" else ""

        val allowsChildren = allowsChildren()
        // TODO: we should look for parameter names in the cases where views have @Children properties that have parameters
        val parameterNames = emptyList<String>()

        val presentableText =
            if (allowsChildren && parameterNames.isNotEmpty())
                "<$name> ${parameterNames.joinToString()} -> ... </$name>"
            else if (allowsChildren)
                "<$name>...</$name>"
            else
                "<$name />"

        val psiClass = this

        val renderer = object : LookupElementRenderer<LookupElement>() {
            override fun renderElement(
                element: LookupElement?,
                presentation: LookupElementPresentation?
            ) {
                if (presentation == null) return
                with(presentation) {
                    itemText = presentableText
                    isItemTextBold = false
                    icon = psiClass.getIcon(Iconable.ICON_FLAG_VISIBILITY)
                    appendTailText(tailText, true)
                }
            }
        }

        return LookupElementBuilder.create(this, name)
            .withRenderer(renderer)
            .withInsertHandler(psiClassTagInsertionHandler)
            .apply {
                putUserData(ALLOWS_CHILDREN, allowsChildren)
                putUserData(PSI_CLASS, this@constructLookupElement)
                putUserData(Weighers.IS_IMPORTED_KEY, false)
            }
    }

    private fun FqName.constructLookupElement(): LookupElement? {
        val renderer = object : LookupElementRenderer<LookupElement>() {
            override fun renderElement(
                element: LookupElement?,
                presentation: LookupElementPresentation?
            ) {
                with(presentation ?: return) {
                    itemText = "<${this@constructLookupElement}.* />"
                    itemTextForeground = JBColor.lightGray
                    icon = PlatformIcons.PACKAGE_ICON
                }
            }
        }

        return LookupElementBuilder.create(pathSegments().last().asString())
            .withRenderer(renderer)
            .withInsertHandler(packageInsertionHandler)
            .apply {
                putUserData(Weighers.IS_IMPORTED_KEY, false)
            }
    }

    private enum class ComposableKind(val valid: Boolean) {
        // This is used internally. public functions should never return this as a result.
        UNKNOWN(false),
        // This type cannot be used in a tag because it is either uninvokable, or invokable and returning a type which is itself "INVALID"
        INVALID(false),
        // This type can be used in a tag but the "terminal" invoke (the one returning Unit) is not marked as @Composable
        UNIT_TERMINAL(true),
        // This type can be used in a tag because it is either invokable and marked as @Composable, or invokable and returning a type
        // which is itself "COMPOSABLE"
        COMPOSABLE(true)
    }

    private inner class ValidTagHelper {
        private val index = mutableMapOf<KotlinType, ComposableKind>()
        private val invokeFns = mutableMapOf<KotlinType, MutableList<KotlinType>>()

        fun getComposability(type: KotlinType?): ComposableKind {
            if (type == null) return ComposableKind.INVALID
            val kind = index[type] ?: ComposableKind.UNKNOWN
            return when (kind) {
                ComposableKind.UNKNOWN -> {
                    // temporarily set the composability of this type to INVALID, so we don't get into a cycle
                    index[type] =
                        ComposableKind.INVALID

                    // there might be an invoke function that goes to another type, in which case
                    // we need to check the composability of the return type
                    var max = invokeFns[type]
                        ?.map { getComposability(it) }
                        ?.maxBy { it.ordinal }
                        ?: ComposableKind.INVALID

                    // if we are still invalid, we check to see if any invoke functions are defined on the
                    // type. This is needed because the indicesHelper doesn't seem to go through classes in
                    // other modules
                    if (max == ComposableKind.INVALID &&
                        type.memberScope.getFunctionNames().contains(INVOKE)) {
                        type
                            .memberScope
                            .getContributedFunctions(INVOKE, NoLookupLocation.FROM_IDE)
                            .filter { it.name == INVOKE && it.isOperator }
                            .forEach { invoke ->
                                addInvoke(type, invoke.returnType
                                    ?: DefaultBuiltIns.Instance.unitType, invoke)
                            }

                        index[type]?.let { max = it }
                    }

                    // It's possible we have new invokeFns now, so we check again
                    if (max == ComposableKind.INVALID) {
                        max = invokeFns[type]
                            ?.map { getComposability(it) }
                            ?.maxBy { it.ordinal }
                                ?: ComposableKind.INVALID
                    }

                    // if we are still invalid, we want to check the composability of our immediate
                    // super-types
                    if (max == ComposableKind.INVALID) {
                        max = type
                            .immediateSupertypes()
                            .map { getComposability(it) }
                            .maxBy { it.ordinal }
                                ?: ComposableKind.INVALID
                    }

                    // at this point we've exhausted all options, so we should set the composability
                    // of this type
                    index[type] = max
                    max
                }
                ComposableKind.INVALID,
                ComposableKind.UNIT_TERMINAL,
                ComposableKind.COMPOSABLE -> kind
            }
        }

        fun addInvoke(type: KotlinType, returnType: KotlinType, descriptor: FunctionDescriptor) {
            val isUnit = returnType.isUnit()
            val isComposable = descriptor.hasComposableAnnotation()

            index[type] = when {
                isComposable -> ComposableKind.COMPOSABLE
                isUnit -> ComposableKind.UNIT_TERMINAL
                else -> ComposableKind.UNKNOWN
            }
            if (!isUnit) {
                invokeFns.multiPut(type, returnType)
            }
        }
    }

    private fun ClassDescriptor.isEmittable(): Boolean {
        return emitUpperBounds.any { isSubclassOf(it) }
    }

    private fun CallableDescriptor.isImmediatelyComposable(): Boolean {
        return when (this) {
            is FunctionDescriptor -> returnType?.isUnit() == true && hasComposableAnnotation()
            is VariableDescriptor -> hasComposableAnnotation() || type.hasComposableAnnotation() ||
                    hasChildrenAnnotation() || type.hasChildrenAnnotation()
            else -> false
        }
    }

    private fun collectComposable(
        d: DeclarationDescriptor,
        type: KotlinType?,
        tagHelper: ValidTagHelper
    ) {
        val kind = tagHelper.getComposability(type)
        if (kind == ComposableKind.INVALID) {
            // at this point we can just throw it on the floor?
        } else {
            val element = d.constructLookupElement()
            if (element != null) collector.addElement(element)
        }
    }

    private fun collectDescriptor(d: DeclarationDescriptor, tagHelper: ValidTagHelper) {
        when (d) {
            is ClassDescriptor -> {
                if (d.modality == Modality.ABSTRACT) return
                if (d.kind == ClassKind.INTERFACE) return
                if (!d.isVisibleDescriptor()) return
                if (d.isEmittable()) {
                    val element = d.constructLookupElement()
                    if (element != null) collector.addElement(element)
                } else {
                    collectComposable(d, d.defaultType, tagHelper)
                }
            }
            is CallableDescriptor -> {
                if (d.isImmediatelyComposable()) {
                    val element = d.constructLookupElement()
                    if (element != null) collector.addElement(element)
                } else {
                    val type = when (d) {
                        is FunctionDescriptor -> d.returnType
                        is VariableDescriptor -> d.type
                        else -> null
                    }
                    collectComposable(d, type, tagHelper)
                }
            }
            else -> {
                // TODO(lmr): are there valid cases here?
            }
        }
    }

    override fun doComplete() {
        val receiverText = callTypeAndReceiver.receiver?.text
        val receiverFqName = receiverText?.let { FqName(it) }

        val tagHelper = ValidTagHelper()

        indiceshelper.getMemberOperatorsByName(INVOKE.identifier).forEach { x ->
            val dispatchReceiver = x.dispatchReceiverParameter
            val extensionReceiver = x.extensionReceiverParameter
            if (extensionReceiver == null && dispatchReceiver != null) {
                tagHelper.addInvoke(
                    dispatchReceiver.type,
                    x.returnType ?: DefaultBuiltIns.Instance.unitType,
                    x
                )
            }
        }

        indiceshelper.getTopLevelExtensionOperatorsByName(INVOKE.identifier).forEach { x ->
            val dispatchReceiver = x.dispatchReceiverParameter
            val extensionReceiver = x.extensionReceiverParameter
            if (extensionReceiver != null && dispatchReceiver == null) {
                tagHelper.addInvoke(
                    extensionReceiver.type,
                    x.returnType ?: DefaultBuiltIns.Instance.unitType,
                    x
                )
            }
        }

        if (receiverTypes != null && receiverTypes.isNotEmpty()) {
            receiverTypes
                .flatMap { it.type.memberScope.getContributedDescriptors() }
                .asSequence()
                .forEach { collectDescriptor(it, tagHelper) }
            flushToResultSet()
        }

        if (callTypeAndReceiver.receiver != null) {
            if (receiverFqName != null) {
                module
                    .getPackage(receiverFqName)
                    .memberScope
                    .getContributedDescriptors()
                    .forEach { collectDescriptor(it, tagHelper) }
                flushToResultSet()
            }
        } else {
            doSimpleCompleteNew(tagHelper)
        }

        doPackageNameCompletion()
    }

    private fun doSimpleCompleteNew(tagHelper: ValidTagHelper) {
        // 1. scoped declarations that are marked @Composable or emittable
        scope
            .collectAllFromMeAndParent { s -> s.getContributedDescriptors() }
            .asIterable()
            .forEach { collectDescriptor(it, tagHelper) }

        flushToResultSet()

        // 2. top level kotlin classes that are "emittable"
        //
        // throw away those that we know can't also be valid composables. keep the ones that might be.
        indiceshelper.getKotlinClasses({ true }).forEach { d ->
            if (d.modality == Modality.ABSTRACT) return@forEach
            if (d.kind == ClassKind.INTERFACE) return@forEach
            if (!d.isVisibleDescriptor()) return@forEach
            if (d.isEmittable()) {
                val element = d.constructLookupElement()
                if (element != null) collector.addElement(element)
            } else {
                collectComposable(d, d.defaultType, tagHelper)
            }
        }
        flushToResultSet()

        // 3. top level callables that are marked @Composable
        //
        // As a first order approximation, we will only capture non-nested composables here. keep the rest.
        indiceshelper.processTopLevelCallables({ true }) { d ->
            // if it is a constructor, we will capture it in the getKotlinClasses(...) call above
            if (d is ClassConstructorDescriptor) return@processTopLevelCallables
            if (!d.isVisibleDescriptor()) return@processTopLevelCallables
            if (d.isImmediatelyComposable()) {
                val element = d.constructLookupElement()
                if (element != null) collector.addElement(element)
            } else {
                val type = when (d) {
                    is FunctionDescriptor -> d.returnType
                    is VariableDescriptor -> d.type
                    else -> null
                }
                collectComposable(d, type, tagHelper)
            }
        }
        flushToResultSet()

        // 4. top level java classes that are "emittable"
        AllClassesGetter.processJavaClasses(
            prefixMatcher,
            project,
            searchScope
        ) { psiClass: PsiClass ->
            if (psiClass is KtLightClass) {
                // Kotlin class should have already been added as kotlin element before
                return@processJavaClasses true
            }
            // TODO(lmr): deal with visibility here somehow?
            if (!psiClass.isEmittable()) {
                // filter out synthetic classes produced by Kotlin compiler
                return@processJavaClasses true
            }
            val element = psiClass.constructLookupElement()
            if (element != null) collector.addElement(element)
            return@processJavaClasses true
        }

        flushToResultSet()
    }

    private fun doPackageNameCompletion() {
        val receiverText = callTypeAndReceiver.receiver?.text
        val receiverFqName = receiverText?.let { FqName(it) }
        val prefixFqName = receiverFqName ?: FqName.ROOT
        val prefixString = receiverFqName?.asString() ?: ""

        val packageNames = PackageIndexUtil.getSubPackageFqNames(
            prefixFqName,
            searchScope,
            project,
            prefixMatcher.asNameFilter()
        ).filter { it.hasIdentifiersOnly() }.toMutableSet()

        psiFacade.findPackage(prefixString)?.getSubPackages(searchScope)?.forEach { psiPackage ->
            val name = psiPackage.name

            if (name?.isIdentifier() == true) {
                if (prefixString.isBlank()) {
                    packageNames.add(FqName(name))
                } else {
                    packageNames.add(FqName("$prefixString.$name"))
                }
            }
        }

        packageNames.forEach {
            val element = it.constructLookupElement()
            if (element != null) {
                collector.addElement(element)
            }
        }

        flushToResultSet()
    }

    override fun createSorter(): CompletionSorter {
        return super.createSorter()
            .weighBefore(
                KindWeigher.toString(),
                Weighers.IsImported
            )
    }

    private val tagInsertionHandler = InsertHandler<LookupElement> { context, item ->
        val descriptor = item.getUserData(DESCRIPTOR) ?: return@InsertHandler
        val allowsChildren = item.getUserData(ALLOWS_CHILDREN) ?: false
        val prefix = callTypeAndReceiver.receiver?.text?.let { "$it." } ?: ""
        val tagName = "$prefix${item.lookupString}"
        insertHandler(descriptor, context, allowsChildren, tagName)
    }

    private val psiClassTagInsertionHandler = InsertHandler<LookupElement> { context, item ->
        val psiClass = item.getUserData(PSI_CLASS) ?: return@InsertHandler
        val descriptor = psiClass.getJavaClassDescriptor(resolutionFacade) ?: return@InsertHandler
        val allowsChildren = item.getUserData(ALLOWS_CHILDREN) ?: false
        val prefix = callTypeAndReceiver.receiver?.text?.let { "$it." } ?: ""
        val tagName = "$prefix${item.lookupString}"
        insertHandler(descriptor, context, allowsChildren, tagName)
    }

    private val packageInsertionHandler = InsertHandler<LookupElement> { context, _ ->
        val document = context.document
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitAllDocuments()
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

        val token = context.file.findElementAt(context.startOffset) ?: return@InsertHandler
        val ktxElement = token.parentOfType<KtxElement>() ?: return@InsertHandler
        val tagName = ktxElement.simpleTagName ?: ktxElement.qualifiedTagName
        ?: return@InsertHandler

        val tailOffset = context.tailOffset
        val moveCaret = context.editor.caretModel.offset == tailOffset
        // if the inserted text is at the end of the tag name, we have more to type
        if (tagName.endOffset != context.editor.caretModel.offset) return@InsertHandler

        val textToInsert = "."

        document.insertString(tailOffset, textToInsert)

        if (moveCaret) {
            context.editor.caretModel.moveToOffset(tailOffset + 1)
        }

        AutoPopupController.getInstance(context.project)?.scheduleAutoPopup(context.editor)
    }

    private fun insertHandler(
        descriptor: DeclarationDescriptor,
        context: InsertionContext,
        allowsChildren: Boolean,
        tagName: String
    ) {
        val document = context.document
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)

        psiDocumentManager.commitAllDocuments()
        if (DescriptorUtils.isTopLevelDeclaration(descriptor) &&
            !descriptor.isArtificialImportAliasedDescriptor) {
            ImportInsertHelper.getInstance(context.project)
                .importDescriptor(context.file as KtFile, descriptor)
        }
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

        val token = context.file.findElementAt(context.startOffset) ?: return
        val ktxElement = token.parentOfType<KtxElement>() ?: return
        val mightWrapBody = ktxElement.nextLeaf {
            it !is PsiWhiteSpace && it.textLength > 0
        }?.text != "}"
        val gt = ktxElement.node.findChildByType(KtTokens.GT)
        // if the tag has a GT token, then we don't need to close it
        if (gt != null) return

        val parameterNames: List<String> =
            if (allowsChildren) descriptor.childrenParameterNames() else emptyList()

        val tailOffset = context.tailOffset
        val moveCaret = context.editor.caretModel.offset == tailOffset
        val textToInsert = buildString {
            if (allowsChildren) {
                append(">")
                if (parameterNames.isNotEmpty()) {
                    append(" ${parameterNames.joinToString()} ->")
                    if (!mightWrapBody) append(" ")
                }
                if (!mightWrapBody) append("</$tagName>")
            } else append(" />")
        }

        document.insertString(tailOffset, textToInsert)

        if (moveCaret) {
            val addedSpace = if (!allowsChildren) 1 else 0

            context.editor.caretModel.moveToOffset(tailOffset + addedSpace)

            // TODO: Check if any attributes are valid, and if so, open autocomplete in `attributes` mode
            // since they just created a new tag, they might want to add some attributes.
            // AutoPopupController.getInstance(context.project)?.scheduleAutoPopup(context.editor)
        }
    }

    companion object {
        val ALLOWS_CHILDREN = Key<Boolean>("compose.allows_children")
        val DESCRIPTOR = Key<DeclarationDescriptor>("compose.descriptor")
        val PSI_CLASS = Key<PsiClass>("compose.psiClass")
    }
}

private fun <T, V> MutableMap<T, MutableList<V>>.multiPut(key: T, value: V) {
    val current = get(key)
    if (current != null) {
        current.add(value)
    } else {
        put(key, mutableListOf(value))
    }
}