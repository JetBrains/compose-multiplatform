package org.jetbrains.kotlin.r4a.idea

import com.intellij.psi.PsiElement

internal inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? {
    var node: PsiElement? = this
    while (node != null) {
        if (node is T) return node
        node = node.parent
    }
    return null
}