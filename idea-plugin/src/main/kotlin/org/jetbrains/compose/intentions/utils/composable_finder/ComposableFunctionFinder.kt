package org.jetbrains.compose.intentions.utils.composable_finder

import com.intellij.psi.PsiElement

interface ComposableFunctionFinder {

    fun isFunctionComposable(psiElement: PsiElement): Boolean
}
