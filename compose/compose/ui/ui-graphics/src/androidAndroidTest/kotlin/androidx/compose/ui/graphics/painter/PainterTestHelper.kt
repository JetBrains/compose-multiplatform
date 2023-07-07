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

package androidx.compose.ui.graphics.painter

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Helper method that draws a Painter into the given canvas, automatically creating a DrawScope
 * to do so with
 */
fun drawPainter(
    painter: Painter,
    canvas: Canvas,
    size: Size,
    alpha: Float = 1.0f,
    colorFilter: ColorFilter? = null,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr
) {
    CanvasDrawScope().draw(Density(1.0f, 1.0f), layoutDirection, canvas, size) {
        with(painter) {
            draw(size, alpha = alpha, colorFilter = colorFilter)
        }
    }
}