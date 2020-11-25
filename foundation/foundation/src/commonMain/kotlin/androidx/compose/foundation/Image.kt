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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.ImagePainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale

/**
 * A composable that lays out and draws a given [ImageBitmap]. This will attempt to
 * size the composable according to the [ImageBitmap]'s given width and height. However, an
 * optional [Modifier] parameter can be provided to adjust sizing or draw additional content (ex.
 * background). Any unspecified dimension will leverage the [ImageBitmap]'s size as a minimum
 * constraint.
 *
 * The following sample shows basic usage of an Image composable to position and draw an
 * [ImageBitmap] on screen
 * @sample androidx.compose.foundation.samples.ImageSample
 *
 * For use cases that require drawing a rectangular subset of the [ImageBitmap] consumers can use
 * overload that consumes a [Painter] parameter shown in this sample
 * @sample androidx.compose.foundation.samples.ImagePainterSubsectionSample
 *
 * @param bitmap The [ImageBitmap] to draw.
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param alignment Optional alignment parameter used to place the [ImageBitmap] in the given
 * bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [ImageBitmap].
 * @param alpha Optional opacity to be applied to the [ImageBitmap] when it is rendered onscreen
 * @param colorFilter Optional ColorFilter to apply for the [ImageBitmap] when it is rendered
 * onscreen
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun Image(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    val imagePainter = remember(bitmap) { ImagePainter(bitmap) }
    Image(
        painter = imagePainter,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter
    )
}

/**
 * A composable that lays out and draws a given [ImageVector]. This will attempt to
 * size the composable according to the [ImageVector]'s given width and height. However, an
 * optional [Modifier] parameter can be provided to adjust sizing or draw additional content (ex.
 * background). Any unspecified dimension will leverage the [ImageVector]'s size as a minimum
 * constraint.
 *
 * @sample androidx.compose.foundation.samples.ImageVectorSample
 *
 * @param imageVector The [ImageVector] to draw.
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param alignment Optional alignment parameter used to place the [ImageVector] in the given
 * bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [ImageVector].
 * @param alpha Optional opacity to be applied to the [ImageVector] when it is rendered onscreen
 * @param colorFilter Optional ColorFilter to apply for the [ImageVector] when it is rendered
 * onscreen
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun Image(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) = Image(
    painter = rememberVectorPainter(imageVector),
    modifier = modifier,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter
)

/**
 * Creates a composable that lays out and draws a given [Painter]. This will attempt to size
 * the composable according to the [Painter]'s intrinsic size. However, an optional [Modifier]
 * parameter can be provided to adjust sizing or draw additional content (ex. background)
 *
 * **NOTE** a Painter might not have an intrinsic size, so if no LayoutModifier is provided
 * as part of the Modifier chain this might size the [Image] composable to a width and height
 * of zero and will not draw any content. This can happen for Painter implementations that
 * always attempt to fill the bounds like [ColorPainter]
 *
 * @sample androidx.compose.foundation.samples.ImagePainterSample
 *
 * @param painter to draw
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param alignment Optional alignment parameter used to place the [Painter] in the given
 * bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [Painter].
 * @param alpha Optional opacity to be applied to the [Painter] when it is rendered onscreen
 * the default renders the [Painter] completely opaque
 * @param colorFilter Optional colorFilter to apply for the [Painter] when it is rendered onscreen
 */
@Composable
fun Image(
    painter: Painter,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    // Explicitly use a simple Layout implementation here as Spacer squashes any non fixed
    // constraint with zero
    Layout(
        emptyContent(),
        modifier.clipToBounds().paint(
            painter,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter
        )
    ) { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}