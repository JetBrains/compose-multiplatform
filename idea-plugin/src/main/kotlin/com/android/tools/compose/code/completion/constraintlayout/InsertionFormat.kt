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

import com.android.tools.compose.completion.inserthandler.LiteralNewLineFormat
import com.android.tools.compose.completion.inserthandler.LiteralWithCaretFormat
import com.android.tools.compose.completion.inserthandler.LiveTemplateFormat

internal val JsonStringValueTemplate = LiteralWithCaretFormat(": '|',")

internal val JsonNumericValueTemplate = LiteralWithCaretFormat(": |,")

internal val JsonNewObjectTemplate = LiteralNewLineFormat(": {\n}")

internal val JsonStringArrayTemplate = LiteralWithCaretFormat(": ['|'],")

internal val JsonObjectArrayTemplate = LiteralNewLineFormat(": [{\n}],")

internal val ConstrainAnchorTemplate = LiveTemplateFormat(": ['<>', '<>', <0>],")

internal val ClearAllTemplate = LiteralWithCaretFormat(
  literalFormat = ": ['${ClearOption.Constraints}', '${ClearOption.Dimensions}', '${ClearOption.Transforms}'],"
)

/**
 * Returns a [LiveTemplateFormat] that contains a template for a Json array with numeric type, where the size of the array is given by
 * [count] and the user may edit each of the values in the array using Live Templates.
 *
 * E.g.: For [count] = 3, returns the template: `": [0, 0, 0],"`, where every value may be changed by the user.
 */
internal fun buildJsonNumberArrayTemplate(count: Int): LiveTemplateFormat {
  val times = count.coerceAtLeast(1)
  return LiveTemplateFormat(": [" + "<0>, ".repeat(times).removeSuffix(", ") + "],")
}