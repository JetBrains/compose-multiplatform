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
package com.android.tools.compose.intentions

import com.android.tools.compose.ComposeBundle
import com.android.tools.idea.flags.StudioFlags
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


/**
 * Removes wrappers like Row, Column and Box around widgets.
 */
class ComposeUnwrapAction : IntentionAction {
  private val WRAPPERS_FQ_NAMES = setOf(
    "androidx.compose.foundation.layout.Box",
    "androidx.compose.foundation.layout.Row",
    "androidx.compose.foundation.layout.Column"
  )

  override fun startInWriteAction() = true

  override fun getText() = ComposeBundle.message("remove.wrapper")

  override fun getFamilyName() = ComposeBundle.message("remove.wrapper")

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    return when {
      !StudioFlags.COMPOSE_EDITOR_SUPPORT.get() -> false
      file == null || editor == null -> false
      !file.isWritable || file !is KtFile -> false
      else -> isCaretAtWrapper(editor, file)
    }
  }

  private fun isCaretAtWrapper(editor: Editor, file: PsiFile): Boolean {
    val elementAtCaret = file.findElementAt(editor.caretModel.offset)?.parentOfType<KtNameReferenceExpression>() ?: return false
    val name = elementAtCaret.resolve().safeAs<KtNamedFunction>()?.fqName?.asString() ?: return false
    return WRAPPERS_FQ_NAMES.contains(name)
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    if (file == null || editor == null) return
    val wrapper = file.findElementAt(editor.caretModel.offset)?.parentOfType<KtNameReferenceExpression>() ?: return
    val outerBlock = wrapper.parent.safeAs<KtCallExpression>() ?: return
    val lambdaBlock = PsiTreeUtil.findChildOfType(outerBlock, KtBlockExpression::class.java, true) ?: return
    outerBlock.replace(lambdaBlock)
  }

}