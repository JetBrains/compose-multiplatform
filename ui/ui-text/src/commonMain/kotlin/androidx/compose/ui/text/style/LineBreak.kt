/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.style

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.style.LineBreak.Companion.Heading
import androidx.compose.ui.text.style.LineBreak.Companion.Paragraph
import androidx.compose.ui.text.style.LineBreak.Companion.Simple

/**
 * When soft wrap is enabled and the width of the text exceeds the width of its container,
 * line breaks are inserted in the text to split it over multiple lines.
 *
 * There are a number of parameters that affect how the line breaks are inserted.
 * For example, the breaking algorithm can be changed to one with improved readability
 * at the cost of speed.
 * Another example is the strictness, which in some languages determines which symbols can appear
 * at the start of a line.
 *
 * `LineBreak` represents a configuration for line breaking, offering several presets
 * for different use cases: [Simple], [Heading], [Paragraph].
 *
 * @sample androidx.compose.ui.text.samples.LineBreakSample
 *
 * For further customization, each platform has its own parameters. An example on Android:
 *
 * @sample androidx.compose.ui.text.samples.AndroidLineBreakSample
 */
@Immutable
expect value class LineBreak private constructor(
    private val mask: Int
) {
    companion object {
        /**
         * Basic, fast line breaking. Ideal for text input fields, as it will cause minimal
         * text reflow when editing.
         */
        @Stable
        val Simple: LineBreak

        /**
         * Looser breaking rules, suitable for short text such as titles or narrow newspaper
         * columns. For longer lines of text, use [Paragraph] for improved readability.
         */
        @Stable
        val Heading: LineBreak

        /**
         * Slower, higher quality line breaking for improved readability.
         * Suitable for larger amounts of text.
         */
        @Stable
        val Paragraph: LineBreak
    }
}