package androidx.compose.plugins.idea.quickfix

import com.intellij.codeInsight.ImportFilter
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.util.getResolveScope
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.getFileResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtxElement
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.idea.parentOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable

class ImportComposerFix(
    element: KtExpression,
    private val ktxElement: KtxElement
) : ComposeImportFix(element) {

    override fun computeSuggestions(): List<ImportVariant> {
        if (!ktxElement.isValid) return emptyList()

        if (ktxElement.containingFile !is KtFile) return emptyList()

        val file = ktxElement.containingKtFile

        val bindingContext = ktxElement.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)

        val searchScope = getResolveScope(file)

        val resolutionFacade = file.getResolutionFacade()

        val topLevelScope = resolutionFacade.getFileResolutionScope(file)

        fun isVisible(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is DeclarationDescriptorWithVisibility) {
                return descriptor.isVisible(
                    ktxElement,
                    null,
                    bindingContext,
                    resolutionFacade
                )
            }

            return true
        }

        fun shouldShow(descriptor: DeclarationDescriptor): Boolean {
            if (!ImportFilter.shouldImport(file, descriptor.fqNameSafe.asString())) return false

            if (isAlreadyImported(descriptor, topLevelScope, descriptor.fqNameSafe)) return false

            return true
        }

        val candidates = mutableListOf<ImportVariant>()

        val indicesHelper = KotlinIndicesHelper(
            resolutionFacade,
            searchScope,
            ::isVisible,
            file = file
        )

        val composerName = KtxNameConventions.COMPOSER.identifier

        indicesHelper.processTopLevelCallables({ it == composerName }) loop@{ descriptor ->
            if (descriptor.name != KtxNameConventions.COMPOSER) return@loop
            if (!isVisible(descriptor)) return@loop
            if (!shouldShow(descriptor)) return@loop
            candidates.add(
                ImportVariant.forComponent(
                    descriptor
                )
            )
        }

        return candidates
    }

    private fun isAlreadyImported(
        target: DeclarationDescriptor,
        topLevelScope: LexicalScope,
        targetFqName: FqName
    ): Boolean {
        val name = target.name
        return when (target) {
            is ClassifierDescriptorWithTypeParameters -> {
                val classifier = topLevelScope.findClassifier(name, NoLookupLocation.FROM_IDE)
                classifier?.importableFqName == targetFqName
            }

            is FunctionDescriptor ->
                topLevelScope.findFunction(name, NoLookupLocation.FROM_IDE) {
                    it.importableFqName == targetFqName
                } != null

            is PropertyDescriptor ->
                topLevelScope.findVariable(name, NoLookupLocation.FROM_IDE) {
                    it.importableFqName == targetFqName
                } != null

            else -> false
        }
    }

    companion object MyFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement as? KtExpression ?: return null
            val ktxElement = element.parentOfType<KtxElement>() ?: return null
            return ImportComposerFix(element, ktxElement).apply { collectSuggestions() }
        }
    }
}