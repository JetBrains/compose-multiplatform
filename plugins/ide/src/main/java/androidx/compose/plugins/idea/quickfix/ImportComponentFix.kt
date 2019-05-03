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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtxElement
import androidx.compose.plugins.idea.parentOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable

class ImportComponentFix(
    element: KtSimpleNameExpression,
    private val ktxElement: KtxElement
) : ComposeImportFix(element) {

    private val name = element.getReferencedName()

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

        indicesHelper.processTopLevelCallables({ it == name }) loop@{ descriptor ->
            if (!isVisible(descriptor)) return@loop
            if (!shouldShow(descriptor)) return@loop
            // NOTE(lmr): we could restrict some options here, but it might be best to include everything and then let
            // the next level of error resolution tell the user how to fix. For instance, if we checked for @Composable, we might
            // miss out on a freshly created component function that the user wants to use, but hasn't annotated properly
            // yet. In that case we might still want to include it in this list, since duplicate names will be rare.
            candidates.add(
                ImportVariant.forComponent(
                    descriptor
                )
            )
        }

        indicesHelper.getJvmClassesByName(name).forEach { descriptor ->
            if (!isVisible(descriptor)) return@forEach
            if (!shouldShow(descriptor)) return@forEach
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
            val element = diagnostic.psiElement as? KtSimpleNameExpression ?: return null
            val ktxElement = element.parentOfType<KtxElement>() ?: return null
            val tagOpen = ktxElement.simpleTagName /* ?: ktxElement.qualifiedTagName */
                ?: return null
            if (tagOpen.textRange.contains(element.textRange)) {
                return ImportComponentFix(
                    element,
                    ktxElement
                ).apply { collectSuggestions() }
            }
            val tagClose =
                ktxElement.simpleClosingTagName /* ?: ktxElement.qualifiedClosingTagName */
                    ?: return null
            if (tagClose.textRange.contains(element.textRange)) {
                return ImportComponentFix(
                    element,
                    ktxElement
                ).apply { collectSuggestions() }
            }
            return null
        }
    }
}