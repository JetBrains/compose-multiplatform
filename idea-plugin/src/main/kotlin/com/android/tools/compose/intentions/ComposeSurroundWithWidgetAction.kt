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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.prevLeaf
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.popup.list.ListPopupImpl
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.util.isLineBreak
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

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
 * Finds the first [KtCallExpression] at the given offset stopping if it finds any [KtNamedFunction] so it does not
 * exit the `Composable`.
 */
private fun PsiFile.findParentCallExpression(offset: Int): PsiElement? =
  PsiTreeUtil.findElementOfClassAtOffsetWithStopSet(this, offset, KtCallExpression::class.java,
                                                    false, KtNamedFunction::class.java)

/**
 * Finds the nearest surroundable [PsiElement] starting at the given offset and looking at the parents. If the offset is at
 * the end of a line, this method might look in the immediately previous offset.
 */
private fun findNearestSurroundableElement(file: PsiFile, offset: Int): PsiElement? {
  val nearestElement = file.findElementAt(offset)?.let {
    if (it.isLineBreak()) {
      file.findParentCallExpression(it.prevLeaf(true)?.startOffset ?: (offset - 1))
    }
    else it
  } ?: return null

  return file.findParentCallExpression(nearestElement.startOffset)
}

/**
 * Finds the [TextRange] to surround based on the current [editor] selection. It returns null if there is no block that
 * can be selected.
 */
fun findSurroundingSelectionRange(file: PsiFile, editor: Editor): TextRange? {
  if (!editor.selectionModel.hasSelection()) return null

  // We try to select full call elements to avoid the selection falling in the middle of, for example, a string.
  // This way, selecting the middle of two strings would still wrap the parent calls like for the following example:
  //
  // Text("Hello <selection>world!")
  // Button(...)
  // Text("By</selection>e")
  //
  // Would wrap the three elements instead of just the Button.
  val startSelectionOffset = findNearestSurroundableElement(file, editor.selectionModel.selectionStart)?.startOffset ?: Int.MAX_VALUE
  val endSelectionOffset = findNearestSurroundableElement(file, editor.selectionModel.selectionEnd)?.endOffset ?: -1

  val statements = CodeInsightUtils.findElements(file,
                                                 minOf(editor.selectionModel.selectionStart, startSelectionOffset),
                                                 maxOf(editor.selectionModel.selectionEnd, endSelectionOffset),
                                                 CodeInsightUtils.ElementKind.EXPRESSION)
    .filter { it.isInsideComposableCode() }
  if (statements.isNotEmpty()) {
    return TextRange.create(statements.minOf { it.startOffset }, statements.maxOf { it.endOffset })
  }
  return null
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

  private fun findSurroundableRange(file: PsiFile, editor: Editor): TextRange? = if (editor.selectionModel.hasSelection()) {
    findSurroundingSelectionRange(file, editor)
  }
  else {
    findNearestSurroundableElement(file, editor.caretModel.offset)?.textRange
  }

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = when {
      !StudioFlags.COMPOSE_EDITOR_SUPPORT.get() -> false
      file == null || editor == null -> false
      !file.isWritable || file !is KtFile -> false
      else -> findSurroundableRange(file, editor) != null
  }

  protected abstract fun getTemplate(): TemplateImpl?

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    if (editor == null || file == null) return

    val surroundRange = findSurroundableRange(file, editor) ?: return
    // Extend the selection if it does not match the inferred range
    if (editor.selectionModel.selectionStart != surroundRange.startOffset ||
      editor.selectionModel.selectionEnd != surroundRange.endOffset) {
      editor.selectionModel.setSelection(surroundRange.startOffset, surroundRange.endOffset)
    }
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

