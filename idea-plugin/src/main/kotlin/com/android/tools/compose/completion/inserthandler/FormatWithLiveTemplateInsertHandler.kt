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
import com.intellij.codeInsight.daemon.impl.quickfix.EmptyExpression
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode

/**
 * Handles insertions of an [InsertionFormat] using the [TemplateManager], stopping at every '<>' for user input.
 */
class FormatWithLiveTemplateInsertHandler(private val format: InsertionFormat) : InsertHandler<LookupElement> {
  override fun handleInsert(context: InsertionContext, item: LookupElement) {
    val templateManager = TemplateManager.getInstance(context.project)
    val template = templateManager.createTemplate("", "")

    // Create template from the given format
    getTemplateSegments(format).forEach { segment ->
      val text = segment.textSegment
      if (segment.takesUserInput) {
        if (text.isNotEmpty()) {
          template.addVariable(ConstantNode(text), true)
        }
        else {
          template.addVariable(EmptyExpression(), true)
        }
      }
      else {
        template.addTextSegment(text)
      }
    }

    templateManager.startTemplate(context.editor, template)
  }
}

/**
 * Extracts the insertable text segments from the [InsertionFormat] indicating whether each segment is simple text or if it expects user
 * input.
 */
private fun getTemplateSegments(format: InsertionFormat): List<LiveTemplateSegment> {
  val segments = mutableListOf<LiveTemplateSegment>()
  val templateText = format.insertableString
  var start = 0
  var end = 0
  // Normal text does not take user input
  var isNormalText = true

  while (end < templateText.length) {
    val currentChar = templateText.elementAtOrNull(end)
    if (currentChar == '<' || currentChar == '>') {
      // Stop at the marker characters and add any pending segment
      segments.add(LiveTemplateSegment(takesUserInput = !isNormalText, templateText.substring(start, end)))
      isNormalText = currentChar == '>'
      start = end + 1  // update start but skip this char
    }
    end++
  }
  if (end - start > 1) {
    // Add the last segment if not empty (end index is exclusive)
    segments.add(LiveTemplateSegment(takesUserInput = !isNormalText, templateText.substring(start, end)))
  }

  return segments
}

private data class LiveTemplateSegment(
  val takesUserInput: Boolean,
  val textSegment: String
)