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

package androidx.compose.testutils

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import org.junit.Assert

/**
 * A helper function to run asserts on [Bitmap].
 *
 * @param expectedSize The expected size of the bitmap. Leave null to skip the check.
 * @param expectedColorProvider Returns the expected color for the provided pixel position.
 * The returned color is then asserted as the expected one on the given bitmap.
 *
 * @throws AssertionError if size or colors don't match.
 */
fun ImageBitmap.assertPixels(
    expectedSize: IntSize? = null,
    expectedColorProvider: (pos: IntOffset) -> Color?
) {
    if (expectedSize != null) {
        if (width != expectedSize.width || height != expectedSize.height) {
            throw AssertionError(
                "Bitmap size is wrong! Expected '$expectedSize' but got " +
                    "'$width x $height'"
            )
        }
    }

    val pixel = toPixelMap()
    for (x in 0 until width) {
        for (y in 0 until height) {
            val pxPos = IntOffset(x, y)
            val expectedClr = expectedColorProvider(pxPos)
            if (expectedClr != null) {
                pixel.assertPixelColor(expectedClr, x, y)
            }
        }
    }
}

/**
 * Asserts that the color at a specific pixel in the bitmap at ([x], [y]) is [expected].
 */
fun PixelMap.assertPixelColor(
    expected: Color,
    x: Int,
    y: Int,
    error: (Color) -> String = { color -> "Pixel($x, $y) expected to be $expected, but was $color" }
) {
    val color = this[x, y]
    val errorString = error(color)
    Assert.assertEquals(errorString, expected.red, color.red, 0.02f)
    Assert.assertEquals(errorString, expected.green, color.green, 0.02f)
    Assert.assertEquals(errorString, expected.blue, color.blue, 0.02f)
    Assert.assertEquals(errorString, expected.alpha, color.alpha, 0.02f)
}

/**
 * Asserts that the expected color is present in this bitmap.
 *
 * @throws AssertionError if the expected color is not present.
 */
fun ImageBitmap.assertContainsColor(
    expectedColor: Color
): ImageBitmap {
    if (!containsColor(expectedColor)) {
        throw AssertionError("The given color $expectedColor was not found in the bitmap.")
    }
    return this
}

private fun ImageBitmap.containsColor(expectedColor: Color): Boolean {
    val pixels = this.toPixelMap()
    for (x in 0 until width) {
        for (y in 0 until height) {
            val color = pixels[x, y]
            if (color == expectedColor) {
                return true
            }
        }
    }
    return false
}

/**
 * Tests to see if the given point is within the path. (That is, whether the
 * point would be in the visible portion of the path if the path was used
 * with [Canvas.clipPath].)
 *
 * The `point` argument is interpreted as an offset from the origin.
 *
 * Returns true if the point is in the path, and false otherwise.
 */
fun Path.contains(offset: Offset): Boolean {
    val path = android.graphics.Path()
    path.addRect(
        offset.x - 0.01f,
        offset.y - 0.01f,
        offset.x + 0.01f,
        offset.y + 0.01f,
        android.graphics.Path.Direction.CW
    )
    if (path.op(asAndroidPath(), android.graphics.Path.Op.INTERSECT)) {
        return !path.isEmpty
    }
    return false
}

/**
 * Asserts that the given [shape] is drawn within the bitmap with the size the dimensions
 * [shapeSizeX] x [shapeSizeY], centered at ([centerX], [centerY]) with the color [shapeColor].
 * The bitmap area examined is [sizeX] x [sizeY], centered at ([centerX], [centerY]) and everything
 * outside the shape is expected to be color [backgroundColor].
 *
 * @param density current [Density] or the screen
 * @param shape defines the [Shape]
 * @param shapeColor the color of the shape
 * @param backgroundColor the color of the background
 * @param backgroundShape defines the [Shape] of the background
 * @param sizeX width of the area filled with the [backgroundShape]
 * @param sizeY height of the area filled with the [backgroundShape]
 * @param shapeSizeX width of the area filled with the [shape]
 * @param shapeSizeY height of the area filled with the [shape]
 * @param centerX the X position of the center of the [shape] inside the [sizeX]
 * @param centerY the Y position of the center of the [shape] inside the [sizeY]
 * @param shapeOverlapPixelCount The size of the border area from the shape outline to leave it
 * untested as it is likely anti-aliased. The default is 1 pixel
 */
// TODO (mount, malkov) : to investigate why it flakes when shape is not rect
fun ImageBitmap.assertShape(
    density: Density,
    shape: Shape,
    shapeColor: Color,
    backgroundColor: Color,
    backgroundShape: Shape = RectangleShape,
    sizeX: Float = width.toFloat(),
    sizeY: Float = height.toFloat(),
    shapeSizeX: Float = sizeX,
    shapeSizeY: Float = sizeY,
    centerX: Float = width / 2f,
    centerY: Float = height / 2f,
    shapeOverlapPixelCount: Float = 1.0f
) {
    val width = width
    val height = height
    val pixels = toPixelMap()
    Assert.assertTrue(centerX + sizeX / 2 <= width)
    Assert.assertTrue(centerX - sizeX / 2 >= 0.0f)
    Assert.assertTrue(centerY + sizeY / 2 <= height)
    Assert.assertTrue(centerY - sizeY / 2 >= 0.0f)
    val outline = shape.createOutline(Size(shapeSizeX, shapeSizeY), LayoutDirection.Ltr, density)
    val path = Path()
    path.addOutline(outline)
    val shapeOffset = Offset(
        (centerX - shapeSizeX / 2f),
        (centerY - shapeSizeY / 2f)
    )
    val backgroundPath = Path()
    backgroundPath.addOutline(
        backgroundShape.createOutline(Size(sizeX, sizeY), LayoutDirection.Ltr, density)
    )
    for (x in centerX - sizeX / 2 until centerX + sizeX / 2) {
        for (y in centerY - sizeY / 2 until centerY + sizeY / 2) {
            val point = Offset(x.toFloat(), y.toFloat())
            if (!backgroundPath.contains(
                    pixelFartherFromCenter(
                        point,
                        sizeX,
                        sizeY,
                        shapeOverlapPixelCount
                    )
                )
            ) {
                continue
            }
            val offset = point - shapeOffset
            val isInside = path.contains(
                pixelFartherFromCenter(
                    offset,
                    shapeSizeX,
                    shapeSizeY,
                    shapeOverlapPixelCount
                )
            )
            val isOutside = !path.contains(
                pixelCloserToCenter(
                    offset,
                    shapeSizeX,
                    shapeSizeY,
                    shapeOverlapPixelCount
                )
            )
            if (isInside) {
                pixels.assertPixelColor(shapeColor, x, y)
            } else if (isOutside) {
                pixels.assertPixelColor(backgroundColor, x, y)
            }
        }
    }
}

/**
 * Asserts that the bitmap is fully occupied by the given [shape] with the color [shapeColor]
 * without [horizontalPadding] and [verticalPadding] from the sides. The padded area is expected
 * to have [backgroundColor].
 *
 * @param density current [Density] or the screen
 * @param horizontalPadding the symmetrical padding to be applied from both left and right sides
 * @param verticalPadding the symmetrical padding to be applied from both top and bottom sides
 * @param backgroundColor the color of the background
 * @param shapeColor the color of the shape
 * @param shape defines the [Shape]
 * @param shapeOverlapPixelCount The size of the border area from the shape outline to leave it
 * untested as it is likely anti-aliased. The default is 1 pixel
 */
fun ImageBitmap.assertShape(
    density: Density,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    backgroundColor: Color,
    shapeColor: Color,
    shape: Shape = RectangleShape,
    shapeOverlapPixelCount: Float = 1.0f
) {
    val fullHorizontalPadding = with(density) { horizontalPadding.toPx() * 2 }
    val fullVerticalPadding = with(density) { verticalPadding.toPx() * 2 }
    return assertShape(
        density = density,
        shape = shape,
        shapeColor = shapeColor,
        backgroundColor = backgroundColor,
        backgroundShape = RectangleShape,
        shapeSizeX = width.toFloat() - fullHorizontalPadding,
        shapeSizeY = height.toFloat() - fullVerticalPadding,
        shapeOverlapPixelCount = shapeOverlapPixelCount
    )
}

private infix fun Float.until(until: Float): IntRange {
    val from = this.roundToInt()
    val to = until.roundToInt()
    if (from <= Int.MIN_VALUE) return IntRange.EMPTY
    return from..(to - 1)
}

private fun pixelCloserToCenter(
    offset: Offset,
    shapeSizeX: Float,
    shapeSizeY: Float,
    delta: Float
): Offset {
    val centerX = shapeSizeX / 2f
    val centerY = shapeSizeY / 2f
    val d = delta
    val x = when {
        offset.x > centerX -> offset.x - d
        offset.x < centerX -> offset.x + d
        else -> offset.x
    }
    val y = when {
        offset.y > centerY -> offset.y - d
        offset.y < centerY -> offset.y + d
        else -> offset.y
    }
    return Offset(x, y)
}

private fun pixelFartherFromCenter(
    offset: Offset,
    shapeSizeX: Float,
    shapeSizeY: Float,
    delta: Float
): Offset {
    val centerX = shapeSizeX / 2f
    val centerY = shapeSizeY / 2f
    val d = delta
    val x = when {
        offset.x > centerX -> offset.x + d
        offset.x < centerX -> offset.x - d
        else -> offset.x
    }
    val y = when {
        offset.y > centerY -> offset.y + d
        offset.y < centerY -> offset.y - d
        else -> offset.y
    }
    return Offset(x, y)
}