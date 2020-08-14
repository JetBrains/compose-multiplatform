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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * A convenience composable that combines common layout and draw logic.
 *
 * In order to define the size of the [Box], the [androidx.compose.foundation.layout.width],
 * [androidx.compose.foundation.layout.height] and [androidx.compose.foundation.layout.size]
 * modifiers can be used.
 * The [Box] will try to be only as small as its content. However, if it is constrained
 * otherwise, [Box] will allow its content to be smaller and will position the content inside,
 * according to [gravity].
 *
 * The specified [padding] will be applied inside the [Box]. In order to apply padding outside
 * the [Box], the [androidx.compose.foundation.layout.padding] modifier should be used.
 *
 * @sample androidx.compose.foundation.samples.SimpleCircleBox
 *
 * @param modifier The modifier to be applied to the Box
 * @param shape The shape of the box
 * @param backgroundColor The [Color] for background with. If [Color.Transparent], there will be no
 * background
 * @param border [BorderStroke] object that specifies border appearance, such as size and color. If
 * `null`, there will be no border
 * @param padding The padding to be applied inside Box, along its edges. Unless otherwise
 * specified, content will be padded by the [BorderStroke.width], if [border] is provided
 * @param paddingStart sets the padding of the start edge. Setting this will override [padding]
 * for the start edge
 * @param paddingTop sets the padding of the top edge. Setting this will override [padding] for
 * the top edge
 * @param paddingEnd sets the padding of the end edge. Setting this will override [padding] for
 * the end edge
 * @param paddingBottom sets the padding of the bottom edge. Setting this will override [padding]
 * for the bottom edge
 * @param gravity The gravity of the content inside Box
 */
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
    val borderModifier =
        if (border != null) Modifier.border(border, shape) else Modifier
    val backgroundModifier =
        if (backgroundColor != Color.Transparent) {
            Modifier.background(color = backgroundColor, shape = shape)
        } else {
            Modifier
        }
    val paddingModifier =
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
    // TODO(malkov): support ContentColor prorogation (b/148129218)

    val columnArrangement = gravity.toColumnArrangement()
    val columnGravity = gravity.toColumnGravity()
    Column(
        modifier = modifier.then(backgroundModifier).then(borderModifier).then(paddingModifier),
        verticalArrangement = columnArrangement,
        horizontalGravity = columnGravity
    ) {
        children()
    }
}

// TODO(popam/148014745): add a Gravity class consistent with cross axis alignment for Row/Column
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
