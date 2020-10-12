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

package androidx.compose.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Deprecated(
    "All Box parameters have been removed (and gravity has been renamed to alignment). Use " +
        "Modifier.background, Modifier.border and Modifier.padding instead. Also Box has been" +
        " moved to androidx.compose.foundation.layout and that should be used instead.",
    replaceWith = ReplaceWith(
        "Box(\n" +
            "        modifier" +
            "           .background(backgroundColor, shape)" +
            "           .border(border, shape)" +
            "           .padding(" +
            "               start = if (paddingStart != Dp.Unspecified) paddingStart else " +
            "padding," +
            "               top = if (paddingTop != Dp.Unspecified) paddingTop else padding," +
            "               end = if (paddingEnd != Dp.Unspecified) paddingEnd else padding," +
            "               bottom = if (paddingBottom != Dp.Unspecified) paddingBottom else " +
            "padding" +
            "           ),\n" +
            "        gravity,\n" +
            "        children\n" +
            "    )",
        "androidx.compose.foundation.layout.Box",
        "androidx.compose.foundation.background",
        "androidx.compose.foundation.border",
        "androidx.compose.foundation.layout.padding",
        "androidx.compose.ui.unit.Dp"
    )
)
@Suppress("DEPRECATION")
@Composable
fun Box(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    backgroundColor: Color = Color.Transparent,
    border: BorderStroke? = null,
    padding: Dp = border?.width ?: 0.dp,
    paddingStart: Dp = Dp.Unspecified,
    paddingTop: Dp = Dp.Unspecified,
    paddingEnd: Dp = Dp.Unspecified,
    paddingBottom: Dp = Dp.Unspecified,
    gravity: ContentGravity = ContentGravity.TopStart,
    children: @Composable () -> Unit = emptyContent()
) {
    val columnArrangement = gravity.toColumnArrangement()
    val columnGravity = gravity.toColumnGravity()

    Column(
        modifier
            .then(
                if (backgroundColor != Color.Transparent) {
                    Modifier.background(color = backgroundColor, shape = shape)
                } else {
                    Modifier
                }
            )
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .then(
                if (needsPadding(padding, paddingStart, paddingTop, paddingEnd, paddingBottom)) {
                    Modifier.padding(
                        if (paddingStart != Dp.Unspecified) paddingStart else padding,
                        if (paddingTop != Dp.Unspecified) paddingTop else padding,
                        if (paddingEnd != Dp.Unspecified) paddingEnd else padding,
                        if (paddingBottom != Dp.Unspecified) paddingBottom else padding
                    )
                } else {
                    Modifier
                }
            ),
        verticalArrangement = columnArrangement,
        horizontalAlignment = columnGravity
    ) {
        children()
    }
}

@Deprecated("Use Alignment instead", replaceWith = ReplaceWith("Alignment"))
typealias ContentGravity = Alignment

private fun needsPadding(
    padding: Dp,
    paddingStart: Dp,
    paddingTop: Dp,
    paddingEnd: Dp,
    paddingBottom: Dp
) = (padding != Dp.Unspecified && padding != 0.dp) ||
    (paddingStart != Dp.Unspecified && paddingStart != 0.dp) ||
    (paddingTop != Dp.Unspecified && paddingTop != 0.dp) ||
    (paddingEnd != Dp.Unspecified && paddingEnd != 0.dp) ||
    (paddingBottom != Dp.Unspecified && paddingBottom != 0.dp)

private fun Alignment.toColumnArrangement() = Arrangement.aligned(object : Alignment.Vertical {
    override fun align(size: Int): Int = align(IntSize(0, size)).y
})

private fun Alignment.toColumnGravity(): Alignment.Horizontal = object : Alignment.Horizontal {
    override fun align(size: Int, layoutDirection: LayoutDirection): Int {
        return align(IntSize(size, 0), layoutDirection).x
    }
}
