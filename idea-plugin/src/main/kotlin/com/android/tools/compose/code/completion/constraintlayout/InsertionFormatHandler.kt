/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.compose.code.completion.constraintlayout

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.actionSystem.CaretSpecificDataContext
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.psi.PsiDocumentManager

/**
 * An [InsertHandler] to handle [InsertionFormat].
 *
 * The [InsertionFormat] object needs to be present in [LookupElement.getObject] to be handled here.
 */
internal object InsertionFormatHandler : InsertHandler<LookupElement> {
  override fun handleInsert(context: InsertionContext, item: LookupElement) {
    val format = item.`object` as? InsertionFormat ?: return
    when (format) {
      is LiteralWithCaretFormat -> handleCaretInsertion(context, format)
      is LiteralNewLineFormat -> handleNewLineInsertion(context, format)
    }
  }

  /**
   * Handles insertions of [LiteralWithCaretFormat], moving the caret at the position specified by the '|' character.
   */
  private fun handleCaretInsertion(context: InsertionContext, format: LiteralWithCaretFormat) {
    with(context) {
      val isMoveCaret = format.insertableString.contains('|')
      val stringToInsert = format.insertableString.replace("|", "")

      // Insert the string without the reserved character: |
      EditorModificationUtil.insertStringAtCaret(editor, stringToInsert, false, true)
      PsiDocumentManager.getInstance(project).commitDocument(document)

      // Move caret to the position indicated by '|'
      EditorActionUtil.moveCaretToLineEnd(editor, false, true)
      if (isMoveCaret && stringToInsert.isNotEmpty()) {
        val caretPosition = format.insertableString.indexOf('|').coerceAtLeast(0)
        EditorModificationUtil.moveCaretRelatively(editor, caretPosition - stringToInsert.length)
      }
    }
  }

  /**
   * Handles insertions of [LiteralNewLineFormat], applying the new line with the [IdeActions.ACTION_EDITOR_ENTER] and moving the caret at
   * the end of the new line.
   */
  private fun handleNewLineInsertion(context: InsertionContext, format: LiteralNewLineFormat) {
    val literal = format.insertableString
    with(context) {
      val newLineOffset = literal.indexOf('\n')
      val stringToInsert = if (newLineOffset >= 0) {
        StringBuilder(literal).deleteCharAt(newLineOffset).toString()
      }
      else {
        literal
      }
      val moveBy = newLineOffset - stringToInsert.length
      EditorModificationUtil.insertStringAtCaret(editor, stringToInsert, false, true)
      PsiDocumentManager.getInstance(project).commitDocument(document)
      EditorActionUtil.moveCaretToLineEnd(editor, false, true)
      EditorModificationUtil.moveCaretRelatively(editor, moveBy)
      val caret = editor.caretModel.currentCaret
      EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER).execute(
        editor,
        caret,
        CaretSpecificDataContext(DataManager.getInstance().getDataContext(editor.contentComponent), caret)
      )
    }
  }
}