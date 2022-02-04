package org.jetbrains.compose.intentions.utils.isIntentionAvailable

import com.intellij.psi.PsiElement
import org.jetbrains.compose.intentions.utils.composableFinder.ComposableFunctionFinder
import org.jetbrains.kotlin.idea.KotlinLanguage

interface IsIntentionAvailable {

    fun PsiElement.isAvailable(
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
}
