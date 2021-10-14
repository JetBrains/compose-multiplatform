/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3.catalog.library.ui.common

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a stroke border on the inner edges of grid items i.e. bottom as well as end (if not the
 * last item in a row).
 *
 * @param itemIndex The zero-based index of the grid item.
 * @param cellsCount The number of cells (columns for vertical, rows for horizontal) in the grid.
 * @param color The color of the border.
 * @param width The width of the border.
 */
fun Modifier.gridItemBorder(
    itemIndex: Int,
    cellsCount: Int,
    color: Color,
    width: Dp = BorderWidth
) = drawBehind {
    val end = itemIndex.inc().rem(cellsCount) == 0
    drawLine(
        color = color,
        strokeWidth = width.toPx(),
        cap = StrokeCap.Square,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height)
    )
    if (!end) drawLine(
        color = color,
        strokeWidth = width.toPx(),
        cap = StrokeCap.Square,
        start = Offset(size.width, size.height),
        end = Offset(size.width, 0f)
    )
}

/**
 * Composite of local content color at 12% alpha over background color, used by borders.
 */
@Composable
fun compositeBorderColor(): Color = LocalContentColor.current.copy(alpha = BorderAlpha)
    .compositeOver(MaterialTheme.colorScheme.background)

val BorderWidth = 1.dp
private const val BorderAlpha = 0.12f
