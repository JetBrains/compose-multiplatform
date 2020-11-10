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

package androidx.compose.ui.graphics.painter

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

/**
 * [Painter] implementation used to draw an [ImageBitmap] into the provided canvas
 * This implementation can handle applying alpha and [ColorFilter] to it's drawn result
 *
 * @param image The [ImageBitmap] to draw
 * @param srcOffset Optional offset relative to [image] used to draw a subsection of the
 * [ImageBitmap]. By default this uses the origin of [image]
 * @param srcSize Optional dimensions representing size of the subsection of [image] to draw
 * Both the offset and size must have the following requirements:
 *
 * 1) Left and top bounds must be greater than or equal to zero
 * 2) Source size must be greater than zero
 * 3) Source size must be less than or equal to the dimensions of [image]
 */
data class ImagePainter(
    private val image: ImageBitmap,
    private val srcOffset: IntOffset = IntOffset.Zero,
    private val srcSize: IntSize = IntSize(image.width, image.height)
) : Painter() {

    private val size: IntSize = validateSize(srcOffset, srcSize)

    private var alpha: Float = 1.0f

    private var colorFilter: ColorFilter? = null

    override fun DrawScope.onDraw() {
        drawImage(
            image,
            srcOffset,
            srcSize,
            dstSize = IntSize(
                this@onDraw.size.width.roundToInt(),
                this@onDraw.size.height.roundToInt()
            ),
            alpha = alpha,
            colorFilter = colorFilter
        )
    }

    /**
     * Return the dimension of the underlying [ImageBitmap] as it's intrinsic width and height
     */
    override val intrinsicSize: Size get() = size.toSize()

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    private fun validateSize(srcOffset: IntOffset, srcSize: IntSize): IntSize {
        require(
            srcOffset.x >= 0 &&
                srcOffset.y >= 0 &&
                srcSize.width >= 0 &&
                srcSize.height >= 0 &&
                srcSize.width <= image.width &&
                srcSize.height <= image.height
        )
        return srcSize
    }
}