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

/**
 * Describes a string that may be automatically inserted when selecting an autocomplete option.
 */
sealed class InsertionFormat(
  val insertableString: String
)

/**
 * Inserts the string after the auto-completed value.
 *
 * The caret will be moved to the position marked by the '|' character.
 */
class LiteralWithCaretFormat(literalFormat: String) : InsertionFormat(literalFormat)

/**
 * Inserts the string after the auto-complete value.
 *
 * It will insert a new line as if it was done by an ENTER keystroke, marked by the '\n' character.
 *
 * Note that it will only apply the new line on the first '\n' character.
 */
class LiteralNewLineFormat(literalFormat: String) : InsertionFormat(literalFormat)

/**
 * Inserts a string driven by Live templates. The string is inserted after the auto-completed value.
 *
 * Use '<' and '>' to delimit a range of text the user is expected to edit, may contain multiple instances of these delimiters.
 *
 * Eg: For the string `"<0123>, <text>"`. The '0123' will be selected in the editor for the user to modify, once they press Enter, it
 * will select 'text' for the user to modify until all marked snippets of the strings are handled or the user presses ESC to keep the text
 * as is.
 */
class LiveTemplateFormat(templateFormat: String) : InsertionFormat(templateFormat)
