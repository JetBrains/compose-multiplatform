package org.jetbrains.kotlin.r4a

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens.LT
import org.jetbrains.kotlin.lexer.KtTokens.RBRACE
import org.jetbrains.kotlin.psi.*


class R4aCompletionContributor : CompletionContributor() {
    override fun invokeAutoPopup(position: PsiElement, typeChar: Char): Boolean {
        // TODO(lmr): how can we "auto-close" on </ characters?
        when (typeChar) {
            ' ' -> {
                // TODO(lmr): we are missing a number of cases where the value expression is a non-trivial expression
                if (position.parent is KtxElement) {
                    // user is in white space between open/closing tags. attributes are valid here and space indicates intent
                    return true
                }
                if (position.parent?.parent is KtxElement) {
                    // user hit space after a ktx open tag
                    return true
                }
                if (position.parent?.parent is KtxAttribute) {
                    // user hit space after an attribute value
                    return true
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
        return super.invokeAutoPopup(position, typeChar)
    }
}