package org.jetbrains.kotlin.r4a.idea

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

internal inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? {
    var node: PsiElement? = this
    while (node != null) {
        if (node is T) return node
        node = node.parent
    }
    return null
}

private val nonWhiteSpaceFilter = { it: PsiElement ->
    when (it) {
        is PsiWhiteSpace -> false
        is PsiComment -> false
        is KtBlockExpression -> false
        is PsiErrorElement -> false
        else -> true
    }
}
internal fun PsiElement.getNextLeafIgnoringWhitespace(includeSelf: Boolean = false): PsiElement? =
    if (includeSelf && nonWhiteSpaceFilter(this)) this else nextLeaf(nonWhiteSpaceFilter)

internal fun PsiElement.getPrevLeafIgnoringWhitespace(includeSelf: Boolean = false): PsiElement? =
    if (includeSelf && nonWhiteSpaceFilter(this)) this else prevLeaf(nonWhiteSpaceFilter)