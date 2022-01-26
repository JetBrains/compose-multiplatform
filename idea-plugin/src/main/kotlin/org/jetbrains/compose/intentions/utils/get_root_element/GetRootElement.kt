package org.jetbrains.compose.intentions.utils.get_root_element

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtValueArgumentList

/**
 *  KtValueArgumentList -> Parent -> KtNameReferenceExpression -> Parent -> KtCallExpression -> Parent -> KtPropertyDelegate -> Parent -> Property
 *  KtNameReferenceExpression -> Parent -> KtCallExpression ->  Parent -> KtDotQualifiedExpression -> Parent -> KtPropertyDelegate ->  Property
 *  KtNameReferenceExpression -> Parent -> KtCallExpression -> Parent -> KtPropertyDelegate -> Parent -> Property
 *  KtNameReferenceExpression -> Parent -> KtCallExpression -> Parent -> Property
 *  KtNameReferenceExpression -> Parent -> KtCallExpression
 **/
class GetRootElement {

    /**
     * element can be CallExpression (Composable Function) or Property (Composable Property like remember)
     */
    tailrec operator fun invoke(element: PsiElement, iteration: Int = 0): PsiElement? {
        if (iteration > 5) { // fail safe
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
            else -> element
        }
    }
}
