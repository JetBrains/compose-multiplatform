package androidx.compose.plugins.idea.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import androidx.compose.plugins.idea.parentOfType

class ComposeCompletionContributor : CompletionContributor() {

    @Suppress("OverridingDeprecatedMember")
    override fun invokeAutoPopup(position: PsiElement, typeChar: Char): Boolean {
        when (typeChar) {
            ' ' -> {
                if (position.parent?.parent is KtxAttribute) {
                    // user hit space immediately after an attribute value
                    return true
                }
                val ktxElement = position.parentOfType<KtxElement>() ?: return false
                val openTag = ktxElement.simpleTagName ?: ktxElement.qualifiedTagName
                ?: return false
                // user hit space immediately after the open tag
                if (position.endOffset == openTag.endOffset) return true
                if (position is PsiWhiteSpace && position.parent == ktxElement) {
                    val gt = ktxElement.node.findChildByType(KtTokens.GT) ?: return false
                    // user hit space in between attributes
                    if (position.endOffset < gt.startOffset) return true
                }
            }
            '<' -> {
                if (position.parent is KtxElement) {
                    // this also happens inside of a KTX Body in some cases
                    return true
                }
                if (KtPsiUtil.isStatementContainer(position.parent)) {
                    // if the user types a bracket inside of a block, there is a high probability that they are
                    // starting a ktx tag
                    return true
                }
            }
        }
        @Suppress("DEPRECATION")
        return super.invokeAutoPopup(position, typeChar)
    }
}