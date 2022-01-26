package org.jetbrains.compose.intentions.utils.composable_finder

import com.intellij.psi.PsiElement
import org.jetbrains.compose.intentions.utils.is_psi_element_composable.IsPsiElementComposable
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType


class NestedComposableFinder : ComposableFunctionFinder, IsPsiElementComposable {

    override fun isFunctionComposable(psiElement: PsiElement): Boolean {

        if (psiElement is KtCallExpression) {
            psiElement.getChildOfType<KtLambdaArgument>()?.let { lambdaChild ->
                return getComposableFromChildLambda(lambdaChild)
            }
        }

        if (psiElement.parent is KtCallExpression) {
            psiElement.parent.getChildOfType<KtLambdaArgument>()?.let { lambdaChild ->
                return getComposableFromChildLambda(lambdaChild)
            }
        }

        return false
    }

    private fun getComposableFromChildLambda(lambdaArgument: KtLambdaArgument): Boolean {
        val bodyExpression = lambdaArgument.getLambdaExpression()?.functionLiteral?.bodyExpression
        val ktCallExpression = bodyExpression?.getChildOfType<KtCallExpression>() ?: return false
        return ktCallExpression.isComposable()
    }
}
