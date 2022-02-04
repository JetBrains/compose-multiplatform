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
    tailrec operator fun invoke(element: PsiElement, iteration: Int = 0): PsiElement? {
        // To avoid infinite loops
        if (iteration > 5) {
            // Looking for a better way to handle this - throw error or return null
            return null
        }

        return when (element) {
            is KtProperty -> element
            is KtNameReferenceExpression,
            is KtValueArgumentList -> invoke(element.parent, iteration + 1)
            is KtDotQualifiedExpression,
            is KtCallExpression -> {
                when (element.parent) {
                    is KtProperty,
                    is KtDotQualifiedExpression -> invoke(element.parent, iteration + 1) // composable dot expression
                    is KtPropertyDelegate -> invoke(element.parent.parent, iteration + 1) // composable dot expression
                    else -> element
                }
            }
            else -> null
        }
    }
}
