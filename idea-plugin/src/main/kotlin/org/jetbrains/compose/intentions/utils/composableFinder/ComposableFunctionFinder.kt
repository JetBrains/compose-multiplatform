package org.jetbrains.compose.intentions.utils.composableFinder

import com.intellij.psi.PsiElement

interface ComposableFunctionFinder {
    fun isFunctionComposable(psiElement: PsiElement): Boolean
}
