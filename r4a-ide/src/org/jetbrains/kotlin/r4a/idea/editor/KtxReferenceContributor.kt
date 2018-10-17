package org.jetbrains.kotlin.r4a.idea.editor

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.AbstractKotlinReferenceContributor
import org.jetbrains.kotlin.idea.references.KtSimpleReference
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices.KTX_ATTR_INFO

class KtxReferenceContributor : AbstractKotlinReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(KtxAttribute::class.java),
            KtxReferenceProvider
        )
    }
}

object KtxReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
        if (element !is KtxAttribute) return PsiReference.EMPTY_ARRAY
        return arrayOf(AttributeReference(element))
    }
}

class AttributeReference(private var attribute: KtxAttribute) : KtSimpleReference<KtSimpleNameExpression>(attribute.key) {

    override fun getRangeInElement() = TextRange(0, attribute.key.textLength)

    override val resolvesByNames: Collection<Name>
        get() {
            val keyName = attribute.key.getReferencedNameAsName()
            val bindingContext = attribute.analyze()
            val attrInfo = bindingContext[KTX_ATTR_INFO, attribute]
            if (attrInfo != null) {
                val realName = attrInfo.descriptor.name
                if (keyName != realName) {
                    return listOf(keyName, realName)
                }
            }
            return listOf(keyName)
        }
}