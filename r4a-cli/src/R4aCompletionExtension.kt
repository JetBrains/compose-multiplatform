package org.jetbrains.kotlin.r4a

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.completion.KotlinCompletionExtension
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.scopes.utils.collectAllFromMeAndParent
import org.jetbrains.kotlin.types.typeUtil.isUnit

class R4aCompletionExtension : KotlinCompletionExtension() {
    override fun perform(parameters: CompletionParameters, result: CompletionResultSet): Boolean {
        val expr = parameters.position
        val superParent = expr.parent?.parent

        when (superParent) {
            is PsiErrorElement -> {
                if (superParent.firstChild?.node?.elementType == LT) {
                    return performKtxTagCompletion(parameters, result)
                }
            }
            is KtxAttribute -> {
                if (expr.parent?.prevSibling?.node?.elementType == LBRACE) {
                    // we are inside of a ktx expression value... use normal autocomplete
                    return false
                }
                return performKtxAttributeCompletion(parameters, result)
            }
            else -> {
                // do nothing
            }
        }

        return false
    }

    private fun performKtxAttributeCompletion(params: CompletionParameters, result: CompletionResultSet): Boolean {
        val keyExpr = params.position.parent as? KtSimpleNameExpression ?: return false
        val attrExpr = keyExpr.parent as? KtxAttribute ?: return false
        val elementExpr = attrExpr.parent // the element tag could be a PsiErrorElement, so we need to handle resolution ourselves
        val tagNameExpr = elementExpr?.getChildOfType<KtReferenceExpression>()
                ?: return false // the first reference expression will be the tag name

        // TODO(lmr): support qualified identifiers for the tagName
        // TODO(lmr): rank based on whether or not the setter comes from a superclass or not
        // TODO(lmr): rank based on android-specific characteristics? map of common attributes?

        val resolutionFacade = keyExpr.getResolutionFacade()

        val scope = keyExpr.getResolutionScope()

        val module = resolutionFacade.moduleDescriptor

        val declarationDescriptor = R4aUtils.resolveDeclaration(
            expression = tagNameExpr,
            moduleDescriptor = module,
            scopeForFirstPart = scope,
            trace = null
        ) ?: return false


        val possibleAttributes = R4aUtils.getPossibleAttributesForDescriptor(declarationDescriptor, scope, resolutionFacade)

        val usedAttributes = elementExpr.getChildrenOfType<KtxAttribute>()
        val usedAttributesNameSet = usedAttributes.mapNotNull { attr -> attr.key?.getIdentifier()?.text }.toSet()

        val elements = possibleAttributes
            .filter { !usedAttributesNameSet.contains(it.name) }
            .map { attr ->
                val typeString = when (attr.descriptor) {
                    is FunctionDescriptor -> SHORT_NAMES_RENDERER.renderValueParameters(attr.descriptor.valueParameters, false).let { it.substring(1, it.length - 1) }
                    else -> SHORT_NAMES_RENDERER.renderType(attr.type)
                }

                // TODO(lmr): make the elements look pretty!
                LookupElementBuilder
                    .create(attr.descriptor, attr.name)
                    .withPresentableText(attr.name)
                    .withTailText("={${typeString}}", true)
                    .withInsertHandler(ATTRIBUTE_INSERTION_HANDLER)
            }


        result.addAllElements(elements)

        return true
    }

    private fun performKtxTagCompletion(params: CompletionParameters, result: CompletionResultSet): Boolean {
        val position = params.position


        val file = position.containingFile as KtFile

        // TODO: this doesn't handle fully qualified expressions
        val expression = file.findElementAt(params.offset)?.parent as? KtSimpleNameExpression ?: return false

        val resolutionFacade = expression.getResolutionFacade()

        val module = resolutionFacade.moduleDescriptor

        // TODO(lmr): add support for fully-qualified expressions
        // TODO(lmr): add resolution for SFCs
        // TODO(lmr): have a different insertion handler for view groups or components that accept children than for those that don't
        // TODO(lmr): weigh based on proximity or usage???

        // NOTE: we may want to cache this
        val r4aComponentId = ClassId.topLevel(FqName("com.google.r4a.Component"))
        val r4aComponentDescriptor = module.findClassAcrossModuleDependencies(r4aComponentId) ?: return false

        // NOTE: we may want to cache this
        val androidViewId = ClassId.topLevel(FqName("android.view.View"))
        val androidViewDescriptor = module.findClassAcrossModuleDependencies(androidViewId) ?: return false

        val androidViewGroupId = ClassId.topLevel(FqName("android.view.ViewGroup"))
        val androidViewGroupDescriptor = module.findClassAcrossModuleDependencies(androidViewGroupId) ?: return false

        val inScopeElements = expression
            .getResolutionScope()
            .collectAllFromMeAndParent { s -> s.getContributedDescriptors() }
            .filter { d ->
                when (d) {
                    is ClassDescriptor -> d.isSubclassOf(androidViewDescriptor) || d.isSubclassOf(r4aComponentDescriptor)
                    is FunctionDescriptor -> !d.isSuspend && !d.isInline && d.extensionReceiverParameter == null && d.returnType?.isUnit() ?: true
                    else -> false
                }
            }
            .mapNotNull { cd ->
                val psi = cd.findPsi() as? PsiNamedElement

                val allowsChildren = when (cd) {
                    is ClassDescriptor -> cd.isSubclassOf(androidViewGroupDescriptor)
                    else -> false // TODO(lmr): handle @Content annotations on components and SFCs
                }

                if (psi != null) {
                    // TODO(lmr): make the elements look pretty!
                    LookupElementBuilder
                        .create(psi)
                        .withBoldness(true)
                        .withPresentableText("<${cd.name.asString()} />")
                        .withBoldness(false)
                        .appendTailText(" (${cd.fqNameSafe.parent().render()})", true)
                        .withInsertHandler(CLOSE_TAG_INSERTION_HANDLER)
                        .apply { putUserData(ALLOWS_CHILDREN, allowsChildren) }
                } else null
            }

        result.addAllElements(inScopeElements)

        return true
    }

    companion object {
        val ALLOWS_CHILDREN = Key<Boolean>("r4a.allows_children")
        val SHORT_NAMES_RENDERER = DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions { parameterNamesInFunctionalTypes = false }

        private val CLOSE_TAG_INSERTION_HANDLER = InsertHandler<LookupElement> { context, item ->
            val document = context.document
            val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)
            psiDocumentManager.commitAllDocuments()

            val token = context.file.findElementAt(context.startOffset) ?: return@InsertHandler

            val ktxElement = token.parent.parent
            // if the ktx element isn't an "error element", then it must be well-formed... so we don't need to insert the closing bracket!
            if (ktxElement is PsiErrorElement) {
                val gt = ktxElement.node.findChildByType(GT)
                // if the tag has a GT token, then we don't need to close it
                if (gt != null) return@InsertHandler

                val allowsChildren = item.getUserData(ALLOWS_CHILDREN) ?: false

                val tailOffset = context.tailOffset
                val moveCaret = context.editor.caretModel.offset == tailOffset
                val textToInsert = if (allowsChildren) " ></${item.lookupString}>" else " />"
                document.insertString(tailOffset, textToInsert)

                if (moveCaret) {
                    context.editor.caretModel.moveToOffset(tailOffset + 1)

                    // since they just created a new tag, they might want to add some attributes. open up the autocomplete right away!
                    AutoPopupController.getInstance(context.project)?.scheduleAutoPopup(context.editor)
                }
            }
        }

        private val ATTRIBUTE_INSERTION_HANDLER = InsertHandler<LookupElement> { context, item ->
            val document = context.document
            val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)
            psiDocumentManager.commitAllDocuments()

            val token = context.file.findElementAt(context.startOffset) ?: return@InsertHandler
            val ktxAttribute = token.parent.parent as? KtxAttribute ?: return@InsertHandler

            // If the attribute has an equals sign already, then we don't need to insert it...
            if (ktxAttribute.node.findChildByType(KtTokens.EQ) != null) return@InsertHandler

            // NOTE: If we introduce attribute "punning", we will need to do the right thing here...

            val tailOffset = context.tailOffset
            val moveCaret = context.editor.caretModel.offset == tailOffset
            val textToInsert = "={}"
            document.insertString(tailOffset, textToInsert)

            if (moveCaret) {
                // move caret between the braces
                context.editor.caretModel.moveToOffset(tailOffset + 2)

                // since they just created a new attribute and need to fill out the expression, we might as well open up the autocomplete!
                AutoPopupController.getInstance(context.project)?.scheduleAutoPopup(context.editor)
            }
        }
    }
}