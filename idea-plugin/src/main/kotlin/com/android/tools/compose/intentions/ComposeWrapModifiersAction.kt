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
import com.android.tools.compose.formatting.wrapModifierChain
import com.android.tools.compose.isModifierChainLongerThanTwo
import com.android.tools.idea.flags.StudioFlags
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getLastParentOfTypeInRowWithSelf

/**
 * Wraps Modifier(androidx.compose.ui.Modifier) chain that is two modifiers or longer, in one modifier per line.
 */
class ComposeWrapModifiersAction : IntentionAction {
  private companion object {
    val NO_NEW_LINE_BEFORE_DOT = Regex("[^.\\n\\s]\\.")
  }

  override fun startInWriteAction() = true

  override fun getText() = ComposeBundle.message("wrap.modifiers")

  override fun getFamilyName() = ComposeBundle.message("wrap.modifiers")

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    return when {
      !StudioFlags.COMPOSE_EDITOR_SUPPORT.get() -> false
      file == null || editor == null -> false
      !file.isWritable || file !is KtFile -> false
      else -> {
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)?.parentOfType<KtDotQualifiedExpression>()
        val topLevelExpression = elementAtCaret?.getLastParentOfTypeInRowWithSelf<KtDotQualifiedExpression>() ?: return false
        isModifierChainLongerThanTwo(topLevelExpression) &&
        NO_NEW_LINE_BEFORE_DOT.containsMatchIn(topLevelExpression.text)
      }
    }
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    if (file == null || editor == null) return
    val elementAtCaret = file.findElementAt(editor.caretModel.offset)?.parentOfType<KtDotQualifiedExpression>()
    val topLevelExpression = elementAtCaret?.getLastParentOfTypeInRowWithSelf<KtDotQualifiedExpression>() ?: return
    wrapModifierChain(topLevelExpression, CodeStyle.getSettings(file))
  }

}
