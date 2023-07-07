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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.HandlePopup
import androidx.compose.foundation.text.selection.HandleReferencePoint
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.createHandleImage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

private const val Sqrt2 = 1.41421356f
internal val CursorHandleHeight = 25.dp
internal val CursorHandleWidth = CursorHandleHeight * 2f / (1 + Sqrt2)

@Composable
internal actual fun CursorHandle(
    handlePosition: Offset,
    modifier: Modifier,
    content: @Composable (() -> Unit)?
) {
    HandlePopup(
        position = handlePosition,
        handleReferencePoint = HandleReferencePoint.TopMiddle
    ) {
        if (content == null) {
            DefaultCursorHandle(modifier = modifier)
        } else {
            content()
        }
    }
}

@Composable
/*@VisibleForTesting*/
internal fun DefaultCursorHandle(modifier: Modifier) {
    Spacer(modifier.size(CursorHandleWidth, CursorHandleHeight).drawCursorHandle())
}

@Suppress("ModifierInspectorInfo")
internal fun Modifier.drawCursorHandle() = composed {
    val handleColor = LocalTextSelectionColors.current.handleColor
    this.then(
        Modifier.drawWithCache {
            // Cursor handle is the same as a SelectionHandle rotated 45 degrees clockwise.
            val radius = size.width / 2f
            val imageBitmap = createHandleImage(radius = radius)
            val colorFilter = ColorFilter.tint(handleColor)
            onDrawWithContent {
                drawContent()
                withTransform({
                    translate(left = radius)
                    rotate(degrees = 45f, pivot = Offset.Zero)
                }) {
                    drawImage(image = imageBitmap, colorFilter = colorFilter)
                }
            }
        }
    )
}