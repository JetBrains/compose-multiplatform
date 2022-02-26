package org.jetbrains.compose.intentions.utils

import com.intellij.psi.PsiElement
import org.jetbrains.compose.desktop.ide.preview.isComposableFunction
import org.jetbrains.compose.intentions.utils.composableFinder.ComposableFunctionFinder
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

internal fun KtCallExpression.isComposable(): Boolean {
    return getChildOfType<KtNameReferenceExpression>()?.isComposable() ?: false
}

internal fun KtNameReferenceExpression.isComposable(): Boolean {
    val ktNamedFunction = resolve() as? KtNamedFunction ?: return false
    return ktNamedFunction.isComposableFunction()
}

internal fun PsiElement.isIntentionAvailable(
    composableFunctionFinder: ComposableFunctionFinder
): Boolean {
    if (language != KotlinLanguage.INSTANCE) {
        return false
    }

    if (!isWritable) {
        return false
    }

    return parent?.let { parentPsiElement ->
        composableFunctionFinder.isFunctionComposable(parentPsiElement)
    } ?: false
}