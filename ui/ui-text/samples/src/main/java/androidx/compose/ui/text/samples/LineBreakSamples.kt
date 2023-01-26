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

package androidx.compose.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

@Sampled
@Composable
fun LineBreakSample() {
    Text(
        text = "Title of an article",
        style = TextStyle(
            fontSize = 20.sp,
            lineBreak = LineBreak.Heading
        )
    )

    Text(
        text = "A long paragraph in an article",
        style = TextStyle(
            lineBreak = LineBreak.Paragraph
        )
    )
}

@Sampled
@Composable
fun AndroidLineBreakSample() {
    val customTitleLineBreak = LineBreak(
        strategy = LineBreak.Strategy.Simple,
        strictness = LineBreak.Strictness.Loose,
        wordBreak = LineBreak.WordBreak.Default
    )

    Text(
        text = "Title of an article",
        style = TextStyle(
            fontSize = 20.sp,
            lineBreak = customTitleLineBreak
        )
    )

    val defaultStrictnessParagraphLineBreak =
        LineBreak.Paragraph.copy(strictness = LineBreak.Strictness.Default)

    Text(
        text = "A long paragraph in an article",
        style = TextStyle(
            lineBreak = defaultStrictnessParagraphLineBreak
        )
    )
}
