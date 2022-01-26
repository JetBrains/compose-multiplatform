package org.jetbrains.compose.intentions.utils.is_psi_element_composable

import org.jetbrains.compose.desktop.ide.preview.isComposableFunction
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

interface IsPsiElementComposable {

    fun KtCallExpression.isComposable(): Boolean {
        return getChildOfType<KtNameReferenceExpression>()?.isComposable() ?: false
    }

    fun KtNameReferenceExpression.isComposable(): Boolean {
        val ktNamedFunction = resolve() as? KtNamedFunction ?: return false
        return ktNamedFunction.isComposableFunction()
    }
}
