package org.jetbrains.kotlin.r4a

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens.LT
import org.jetbrains.kotlin.lexer.KtTokens.RBRACE
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement


class R4aCompletionContributor : CompletionContributor() {
    override fun invokeAutoPopup(position: PsiElement, typeChar: Char): Boolean {
        // TODO(lmr): how can we "auto-close" on </ characters?
        if (typeChar == '<' && position.parent is KtBlockExpression) {
            // if the user types a bracket inside of a block, there is a high probability that they are
            // starting a ktx tag
            return true
        }
        if (typeChar == ' ' && position.parent is KtxElement) {
            // we are in a Ktx element about to type an attribute
            return true
        }
        if (typeChar == ' ' && position.parent is KtReferenceExpression && position.parent?.prevSibling?.node?.elementType == LT) {
            // we have hit space after the tagname of a ktx open tag
            return true
        }
        if (typeChar == ' ' && position.parent is KtxAttribute && position.node?.elementType == RBRACE) {
            // we have hit space after the closing brace of a ktx attribute
            return true
        }
        return super.invokeAutoPopup(position, typeChar)
    }
}