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
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.psi.PsiDocumentManager

/**
 * Handles insertions of an [InsertionFormat], moving the caret at the position specified by the '|' character.
 */
class FormatWithCaretInsertHandler(private val format: InsertionFormat) : InsertHandler<LookupElement> {
  override fun handleInsert(context: InsertionContext, item: LookupElement) {
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
}