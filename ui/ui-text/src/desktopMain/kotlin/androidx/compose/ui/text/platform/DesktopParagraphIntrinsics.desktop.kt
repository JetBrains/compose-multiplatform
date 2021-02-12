/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.compose.ui.text.platform

import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Density
import org.jetbrains.skija.paragraph.Paragraph
import kotlin.math.ceil

internal actual fun ActualParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    density: Density,
    resourceLoader: Font.ResourceLoader
): ParagraphIntrinsics =
    DesktopParagraphIntrinsics(
        text,
        style,
        spanStyles,
        placeholders,
        density,
        resourceLoader
    )

internal class DesktopParagraphIntrinsics(
    val text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    density: Density,
    resourceLoader: Font.ResourceLoader
) : ParagraphIntrinsics {

    val fontLoader = resourceLoader as FontLoader
    val builder: ParagraphBuilder
    var para: Paragraph
    init {
        builder = ParagraphBuilder(
            fontLoader = fontLoader,
            text = text,
            textStyle = style,
            spanStyles = spanStyles,
            placeholders = placeholders,
            density = density
        )
        para = builder.build()

        para.layout(Float.POSITIVE_INFINITY)
    }

    override val minIntrinsicWidth = ceil(para.getMinIntrinsicWidth())
    override val maxIntrinsicWidth = ceil(para.getMaxIntrinsicWidth())
}
