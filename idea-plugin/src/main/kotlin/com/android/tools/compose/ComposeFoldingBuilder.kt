/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.compose

import com.android.tools.modules.*
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * Adds a folding region for a Modifier chain longer than two.
 */
class ComposeFoldingBuilder : CustomFoldingBuilder() {
  override fun buildLanguageFoldRegions(descriptors: MutableList<FoldingDescriptor>, root: PsiElement, document: Document, quick: Boolean) {
    if (root !is KtFile || DumbService.isDumb(root.project) || !root.inComposeModule()) {
      return
    }

    val composableFunctions = root.getChildrenOfType<KtNamedFunction>().filter { it.isComposableFunction() }

    for (function in composableFunctions) {
      val modifiersChains = PsiTreeUtil.findChildrenOfType(function, KtDotQualifiedExpression::class.java).filter {
        it.parent !is KtDotQualifiedExpression &&
        isModifierChainLongerThanTwo(it)
      }

      for (modifierChain in modifiersChains) {
        descriptors.add(FoldingDescriptor(modifierChain.node, modifierChain.node.textRange))
      }
    }

  }

  /**
   * For Modifier.adjust().adjust() -> Modifier.(...)
   */
  override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String {
    return node.text.substringBefore(".").trim() + ".(...)"
  }

  override fun isRegionCollapsedByDefault(node: ASTNode): Boolean = false
}