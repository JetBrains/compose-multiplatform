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

/**
 * Describes a string that may be automatically inserted when selecting an autocomplete option.
 */
internal sealed class InsertionFormat(
  val insertableString: String
)

/**
 * Inserts the string after the auto-completed value.
 *
 * The caret will be moved to the position marked by the '|' character.
 */
internal class LiteralWithCaretFormat(literalFormat: String) : InsertionFormat(literalFormat)

/**
 * Inserts the string after the auto-complete value.
 *
 * It will insert a new line as if it was done by an ENTER keystroke, marked by the '\n' character.
 *
 * Note that it will only apply the new line on the first '\n' character.
 */
internal class LiteralNewLineFormat(literalFormat: String) : InsertionFormat(literalFormat)

internal val JsonStringValueTemplate = LiteralWithCaretFormat(": '|',")

internal val JsonNewObjectTemplate = LiteralNewLineFormat(": {\n}")
