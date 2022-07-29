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
package com.android.tools.compose.completion

import com.android.tools.compose.completion.inserthandler.FormatWithCaretInsertHandler
import com.android.tools.compose.completion.inserthandler.FormatWithLiveTemplateInsertHandler
import com.android.tools.compose.completion.inserthandler.FormatWithNewLineInsertHandler
import com.android.tools.compose.completion.inserthandler.InsertionFormat
import com.android.tools.compose.completion.inserthandler.LiteralNewLineFormat
import com.android.tools.compose.completion.inserthandler.LiteralWithCaretFormat
import com.android.tools.compose.completion.inserthandler.LiveTemplateFormat
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder

/**
 * Utility function to simplify adding [com.intellij.codeInsight.lookup.LookupElement]s with [InsertionFormat] support.
 *
 * Note that the added lookup element is case-sensitive.
 *
 * @param lookupString The base text to autocomplete, also used to match the user input with a completion result.
 * @param tailText Grayed out text shown after the LookupElement name, not part of the actual completion.
 * @param format InsertionFormat to handle the rest of the completion. See different implementations of [InsertionFormat] for more.
 */
fun CompletionResultSet.addLookupElement(lookupString: String, tailText: String? = null, format: InsertionFormat? = null) {
  // Populate the lookupObject param to allow multiple LookupElements with the same lookupString, differentiated by the tailText.
  var lookupBuilder = LookupElementBuilder.create(tailText ?: lookupString, lookupString)
  if (format != null) {
    val insertionHandler = when (format) {
      is LiteralWithCaretFormat -> FormatWithCaretInsertHandler(format)
      is LiteralNewLineFormat -> FormatWithNewLineInsertHandler(format)
      is LiveTemplateFormat -> FormatWithLiveTemplateInsertHandler(format)
    }
    lookupBuilder = lookupBuilder.withInsertHandler(insertionHandler)
  }
  lookupBuilder = lookupBuilder.withCaseSensitivity(true)
  if (tailText != null) {
    lookupBuilder = lookupBuilder.withTailText(tailText, true)
  }
  addElement(lookupBuilder)
}