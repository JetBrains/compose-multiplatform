/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.idea

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
    if (includeSelf && nonWhiteSpaceFilter(this)) this else nextLeaf(
        nonWhiteSpaceFilter
    )

internal fun PsiElement.getPrevLeafIgnoringWhitespace(includeSelf: Boolean = false): PsiElement? =
    if (includeSelf && nonWhiteSpaceFilter(this)) this else prevLeaf(
        nonWhiteSpaceFilter
    )