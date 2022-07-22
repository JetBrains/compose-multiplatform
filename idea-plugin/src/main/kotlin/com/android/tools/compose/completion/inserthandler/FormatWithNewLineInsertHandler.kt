/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.tools.compose.completion.inserthandler

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
 * Handles insertions of an [InsertionFormat], applying new a line at the `\n` character.
 *
 * Applies the new line with [IdeActions.ACTION_EDITOR_ENTER] and moves the caret at the end of the new line.
 */
class FormatWithNewLineInsertHandler(private val format: InsertionFormat) : InsertHandler<LookupElement> {
  override fun handleInsert(context: InsertionContext, item: LookupElement) {
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