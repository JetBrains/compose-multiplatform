/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.text.style.TextOverflow

object TextPainter {
    /**
     * Paints the text onto the given canvas.
     *
     * @param canvas a canvas to be drawn
     * @param textLayoutResult a result of text layout
     */
    @OptIn(ExperimentalTextApi::class)
    fun paint(canvas: Canvas, textLayoutResult: TextLayoutResult) {
        val needClipping = textLayoutResult.hasVisualOverflow &&
            textLayoutResult.layoutInput.overflow == TextOverflow.Clip
        if (needClipping) {
            val width = textLayoutResult.size.width.toFloat()
            val height = textLayoutResult.size.height.toFloat()
            val bounds = Rect(Offset.Zero, Size(width, height))
            canvas.save()
            canvas.clipRect(bounds)
        }
        try {
            val brush = textLayoutResult.layoutInput.style.brush
            if (brush != null) {
                textLayoutResult.multiParagraph.paint(
                    canvas,
                    brush,
                    textLayoutResult.layoutInput.style.shadow,
                    textLayoutResult.layoutInput.style.textDecoration
                )
            } else {
                textLayoutResult.multiParagraph.paint(
                    canvas,
                    textLayoutResult.layoutInput.style.color,
                    textLayoutResult.layoutInput.style.shadow,
                    textLayoutResult.layoutInput.style.textDecoration
                )
            }
        } finally {
            if (needClipping) {
                canvas.restore()
            }
        }
    }
}