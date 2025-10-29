package org.jetbrains.compose.intentions.utils.getRootPsiElement

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtValueArgumentList

/**
 * To get the root element of a selected Psi element
 */
class GetRootPsiElement {

    /**
     * @param element can be
     * 1. KtCallExpression, KtNameReferenceExpression - Box()
     * 2. KtDotQualifiedExpression - repeatingAnimation.animateFloat
     * 3. KtProperty - val systemUiController = rememberSystemUiController()
     * 4. KtValueArgumentList - ()
     */
    tailrec operator fun invoke(element: PsiElement): PsiElement? {
        return when (element) {
            is KtProperty -> element
            is KtNameReferenceExpression,
            is KtValueArgumentList -> invoke(element.parent)
            is KtDotQualifiedExpression,
            is KtCallExpression -> {
                when (element.parent) {
                    is KtProperty,
                    is KtDotQualifiedExpression -> invoke(element.parent) // composable dot expression
                    is KtPropertyDelegate -> invoke(element.parent.parent) // composable dot expression
                    else -> element
                }
            }
            else -> null
        }
    }
}
