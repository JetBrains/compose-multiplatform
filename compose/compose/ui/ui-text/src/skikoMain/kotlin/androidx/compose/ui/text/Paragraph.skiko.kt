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

package androidx.compose.ui.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration

actual sealed interface Paragraph {
    actual val width: Float
    actual val height: Float
    actual val minIntrinsicWidth: Float
    actual val maxIntrinsicWidth: Float
    actual val firstBaseline: Float
    actual val lastBaseline: Float
    actual val didExceedMaxLines: Boolean
    actual val lineCount: Int
    actual val placeholderRects: List<Rect?>
    actual fun getPathForRange(start: Int, end: Int): Path
    actual fun getCursorRect(offset: Int): Rect
    actual fun getLineLeft(lineIndex: Int): Float
    actual fun getLineRight(lineIndex: Int): Float
    actual fun getLineTop(lineIndex: Int): Float
    actual fun getLineBottom(lineIndex: Int): Float
    actual fun getLineHeight(lineIndex: Int): Float
    actual fun getLineWidth(lineIndex: Int): Float
    actual fun getLineStart(lineIndex: Int): Int
    actual fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int
    actual fun isLineEllipsized(lineIndex: Int): Boolean
    actual fun getLineForOffset(offset: Int): Int
    actual fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float
    actual fun getParagraphDirection(offset: Int): ResolvedTextDirection
    actual fun getBidiRunDirection(offset: Int): ResolvedTextDirection
    actual fun getLineForVerticalPosition(vertical: Float): Int
    actual fun getOffsetForPosition(position: Offset): Int
    actual fun getBoundingBox(offset: Int): Rect
    actual fun getWordBoundary(offset: Int): TextRange
    actual fun paint(canvas: Canvas, color: Color, shadow: Shadow?, textDecoration: TextDecoration?)
    @ExperimentalTextApi
    actual fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    )
    @ExperimentalTextApi
    actual fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    )
}