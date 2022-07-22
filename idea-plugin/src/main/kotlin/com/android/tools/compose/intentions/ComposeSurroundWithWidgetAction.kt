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
import com.android.tools.compose.isInsideComposableCode
import com.android.tools.idea.flags.StudioFlags
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.impl.IntentionActionGroup
import com.intellij.codeInsight.template.impl.InvokeTemplateAction
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateSettings
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiFile
import com.intellij.ui.popup.list.ListPopupImpl
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement.KotlinStatementSurroundDescriptor
import org.jetbrains.kotlin.psi.KtFile

/**
 * Intention action that includes [ComposeSurroundWithBoxAction], [ComposeSurroundWithRowAction], [ComposeSurroundWithColumnAction].
 *
 * After this action is selected, a new pop-up appears, in which user can choose between actions listed above.
 *
 * @see intentionDescriptions/ComposeSurroundWithWidgetActionGroup/before.kt.template
 *      intentionDescriptions/ComposeSurroundWithWidgetActionGroup/after.kt.template
 */
class ComposeSurroundWithWidgetActionGroup :
  IntentionActionGroup<ComposeSurroundWithWidgetAction>(
    listOf(ComposeSurroundWithBoxAction(), ComposeSurroundWithRowAction(), ComposeSurroundWithColumnAction())
  ) {
  override fun getGroupText(actions: List<ComposeSurroundWithWidgetAction>) =
    ComposeBundle.message("surround.with.widget.intention.text")

  override fun chooseAction(project: Project,
                            editor: Editor,
                            file: PsiFile,
                            actions: List<ComposeSurroundWithWidgetAction>,
                            invokeAction: (ComposeSurroundWithWidgetAction) -> Unit) {
    createPopup(project, actions, invokeAction).showInBestPositionFor(editor)
  }

  private fun createPopup(project: Project,
                          actions: List<ComposeSurroundWithWidgetAction>,
                          invokeAction: (ComposeSurroundWithWidgetAction) -> Unit): ListPopup {

    val step = object : BaseListPopupStep<ComposeSurroundWithWidgetAction>(null, actions) {
      override fun getTextFor(action: ComposeSurroundWithWidgetAction) = action.text

      override fun onChosen(selectedValue: ComposeSurroundWithWidgetAction, finalChoice: Boolean): PopupStep<*>? {
        invokeAction(selectedValue)
        return FINAL_CHOICE
      }
    }

    return ListPopupImpl(project, step)
  }

  override fun getFamilyName() = ComposeBundle.message("surround.with.widget.intention.text")
}

/**
 * Surrounds selected statements inside a @Composable function with a widget.
 *
 * @see intentionDescriptions/ComposeSurroundWithWidgetActionGroup/before.kt.template
 *      intentionDescriptions/ComposeSurroundWithWidgetActionGroup/after.kt.template
 */
abstract class ComposeSurroundWithWidgetAction : IntentionAction, HighPriorityAction {
  override fun getFamilyName() = "Compose Surround With Action"

  override fun startInWriteAction(): Boolean = true

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    when {
      !StudioFlags.COMPOSE_EDITOR_SUPPORT.get() -> return false
      file == null || editor == null -> return false
      !file.isWritable || file !is KtFile || !editor.selectionModel.hasSelection() -> return false
      else -> {
        val element = file.findElementAt(editor.caretModel.offset) ?: return false
        if (!element.isInsideComposableCode()) return false

        val statements = KotlinStatementSurroundDescriptor()
          .getElementsToSurround(file, editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd)

        return statements.isNotEmpty()
      }
    }
  }

  protected abstract fun getTemplate(): TemplateImpl?

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    InvokeTemplateAction(getTemplate(), editor, project, HashSet()).perform()
  }

}

/**
 * Surrounds selected statements inside a @Composable function with Box widget.
 */
class ComposeSurroundWithBoxAction : ComposeSurroundWithWidgetAction() {
  override fun getText(): String = ComposeBundle.message("surround.with.box.intention.text")

  override fun getTemplate(): TemplateImpl? {
    return TemplateSettings.getInstance().getTemplate("W", "AndroidCompose")
  }
}

/**
 * Surrounds selected statements inside a @Composable function with Row widget.
 */
class ComposeSurroundWithRowAction : ComposeSurroundWithWidgetAction() {
  override fun getText(): String = ComposeBundle.message("surround.with.row.intention.text")

  override fun getTemplate(): TemplateImpl? {
    return TemplateSettings.getInstance().getTemplate("WR", "AndroidCompose")
  }
}

/**
 * Surrounds selected statements inside a @Composable function with Column widget.
 */
class ComposeSurroundWithColumnAction : ComposeSurroundWithWidgetAction() {
  override fun getText(): String = ComposeBundle.message("surround.with.column.intention.text")

  override fun getTemplate(): TemplateImpl? {
    return TemplateSettings.getInstance().getTemplate("WC", "AndroidCompose")
  }
}

