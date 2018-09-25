package org.jetbrains.kotlin.r4a.idea.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiLiteral
import com.intellij.ui.JBColor
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.refactoring.hasIdentifiersOnly
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.r4a.idea.parentOfType
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.scopes.utils.collectAllFromMeAndParent
import org.jetbrains.kotlin.types.typeUtil.isUnit

class R4aTagCompletionSession(
    configuration: CompletionSessionConfiguration,
    parameters: CompletionParameters,
    toFromOriginalFileMapper: ToFromOriginalFileMapper,
    resultSet: CompletionResultSet
) : BaseR4aCompletionSession(configuration, parameters, toFromOriginalFileMapper, resultSet) {

    private fun DeclarationDescriptor.isValidTag(): Boolean {
        return when (this) {
            is ClassDescriptor -> when {
                modality == Modality.ABSTRACT -> false
                androidViewDescriptor != null && isSubclassOf(androidViewDescriptor) -> true
                r4aComponentDescriptor != null && isSubclassOf(r4aComponentDescriptor) && this != r4aComponentDescriptor -> true
                else -> false
            }
            is FunctionDescriptor -> !isSuspend &&
                    !isInline &&
                    extensionReceiverParameter == null &&
                    (returnType?.isUnit() ?: true) &&
                    hasComposableAnnotation()
            is VariableDescriptor -> hasComposableAnnotation() || type.hasComposableAnnotation() ||
                    hasChildrenAnnotation() || type.hasChildrenAnnotation()
            else -> false
        }
    }


    private fun PsiClass.isValidTag(): Boolean {
        return when {
            isSyntheticKotlinClass() -> false
            isAnnotationType -> false
            isInterface -> false
            isEnum -> false
            isSubclassOf(androidViewPsiClass) -> true
            else -> false
        }
    }

    private fun PsiClass.isSyntheticKotlinClass(): Boolean {
        if ('$' !in name!!) return false // optimization to not analyze annotations of all classes
        val metadata = modifierList?.findAnnotation(JvmAnnotationNames.METADATA_FQ_NAME.asString())
        return (metadata?.findAttributeValue(JvmAnnotationNames.KIND_FIELD_NAME) as? PsiLiteral)?.value ==
                KotlinClassHeader.Kind.SYNTHETIC_CLASS.id
    }

    private fun DeclarationDescriptor.childrenParameterNames(): List<String> {
        when (this) {
            is ClassDescriptor -> {
                val descriptor =
                    unsubstitutedMemberScope.getContributedDescriptors().firstOrNull { it.hasChildrenAnnotation() }
                        ?: unsubstitutedPrimaryConstructor?.valueParameters?.firstOrNull { it.hasChildrenAnnotation() }
                        ?: return emptyList()

                return when (descriptor) {
                    is ParameterDescriptor -> descriptor.type.functionParameterNames()
                    is PropertyDescriptor -> descriptor.type.functionParameterNames()
                    is FunctionDescriptor -> descriptor.valueParameters.firstOrNull()?.type?.functionParameterNames() ?: emptyList()
                    else -> emptyList()
                }
            }
            is FunctionDescriptor -> {
                val descriptor = valueParameters.firstOrNull { it.hasChildrenAnnotation() } ?: return emptyList()
                return descriptor.type.functionParameterNames()
            }
            is VariableDescriptor -> {
                val childTypeArg = type.arguments.firstOrNull { it.type.hasChildrenAnnotation() } ?: return emptyList()
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
            override fun renderElement(element: LookupElement?, presentation: LookupElementPresentation?) {
                if (presentation == null) return
                with(presentation) {
                    val psiElement = DescriptorToSourceUtilsIde.getAnyDeclaration(project, psiDecl)
                    itemText = presentableText
                    isItemTextBold = isVisible
                    icon = KotlinDescriptorIconProvider.getIcon(descriptor, psiElement, Iconable.ICON_FLAG_VISIBILITY)
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
            override fun renderElement(element: LookupElement?, presentation: LookupElementPresentation?) {
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
            override fun renderElement(element: LookupElement?, presentation: LookupElementPresentation?) {
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

    override fun doComplete() {
        val receiverText = callTypeAndReceiver.receiver?.text
        val receiverFqName = receiverText?.let { FqName(it) }
        val prefixFqName = receiverFqName ?: FqName.ROOT
        val prefixString = receiverFqName?.asString() ?: ""

        if (receiverTypes != null && receiverTypes.isNotEmpty()) {
            collector.addElements(
                receiverTypes
                    .flatMap { it.type.memberScope.getContributedDescriptors() }
                    .asSequence()
                    .filter { it.isValidTag() }
                    .mapNotNull { it.constructLookupElement() }
                    .asIterable()
            )
            flushToResultSet()
        }

        if (callTypeAndReceiver.receiver != null) {
            if (receiverFqName != null) {
                collector.addElements(
                    module
                        .getPackage(receiverFqName)
                        .memberScope
                        .getContributedDescriptors()
                        .asIterable()
                        .filter { it.isValidTag() }
                        .mapNotNull { it.constructLookupElement() }
                )
                flushToResultSet()
            }
        } else {
            collector.addElements(
                scope
                    .collectAllFromMeAndParent { s -> s.getContributedDescriptors() }
                    .asIterable()
                    .filter { it.isValidTag() }
                    .mapNotNull { it.constructLookupElement() }
            )
            flushToResultSet()

            val indiceshelper = KotlinIndicesHelper(
                resolutionFacade,
                searchScope,
                { true },
                filterOutPrivate = true,
                declarationTranslator = { toFromOriginalFileMapper.toSyntheticFile(it) },
                file = file
            )

            indiceshelper.processTopLevelCallables({ true }) { d ->
                if (d.isValidTag()) {
                    val element = d.constructLookupElement()
                    if (element != null) collector.addElement(element)
                }
            }

            flushToResultSet()


            AllClassesGetter.processJavaClasses(prefixMatcher, project, searchScope) { psiClass: PsiClass ->
                if (psiClass is KtLightClass) return@processJavaClasses true // Kotlin class should have already been added as kotlin element before
                if (!psiClass.isValidTag()) return@processJavaClasses true // filter out synthetic classes produced by Kotlin compiler
                val element = psiClass.constructLookupElement()
                if (element != null) collector.addElement(element)
                return@processJavaClasses true
            }

            flushToResultSet()
        }

        val packageNames = PackageIndexUtil.getSubPackageFqNames(prefixFqName, searchScope, project, prefixMatcher.asNameFilter())
            .filter { it.hasIdentifiersOnly() }
            .toMutableSet()

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
        val tagName = ktxElement.simpleTagName ?: ktxElement.qualifiedTagName ?: return@InsertHandler

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
        if (DescriptorUtils.isTopLevelDeclaration(descriptor) && !descriptor.isArtificialImportAliasedDescriptor) {
            ImportInsertHelper.getInstance(context.project)
                .importDescriptor(context.file as KtFile, descriptor)
        }
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

        val token = context.file.findElementAt(context.startOffset) ?: return
        val ktxElement = token.parentOfType<KtxElement>() ?: return
        val gt = ktxElement.node.findChildByType(KtTokens.GT)
        // if the tag has a GT token, then we don't need to close it
        if (gt != null) return

        val parameterNames: List<String> = if (allowsChildren) descriptor.childrenParameterNames() else emptyList()

        val tailOffset = context.tailOffset
        val moveCaret = context.editor.caretModel.offset == tailOffset
        val textToInsert = if (allowsChildren && parameterNames.isNotEmpty()) {
            " > ${parameterNames.joinToString()} -> </$tagName>"
        } else if (allowsChildren) {
            " ></$tagName>"
        } else {
            " />"
        }

        document.insertString(tailOffset, textToInsert)

        if (moveCaret) {
            context.editor.caretModel.moveToOffset(tailOffset + 1)

            // since they just created a new tag, they might want to add some attributes. open up the autocomplete right away!
            AutoPopupController.getInstance(context.project)?.scheduleAutoPopup(context.editor)
        }
    }

    companion object {
        val ALLOWS_CHILDREN = Key<Boolean>("r4a.allows_children")
        val DESCRIPTOR = Key<DeclarationDescriptor>("r4a.descriptor")
        val PSI_CLASS = Key<PsiClass>("r4a.psiClass")
    }
}