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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageAsset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageAsset
import androidx.compose.ui.test.assertContainsColor
import androidx.compose.ui.test.assertPixelColor
import androidx.compose.ui.test.assertPixels
import androidx.compose.ui.test.assertShape
import androidx.compose.ui.test.contains
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * A helper function to run asserts on [Bitmap].
 *
 * @param expectedSize The expected size of the bitmap. Leave null to skip the check.
 * @param expectedColorProvider Returns the expected color for the provided pixel position.
 * The returned color is then asserted as the expected one on the given bitmap.
 *
 * @throws AssertionError if size or colors don't match.
 */
@Suppress("DEPRECATION")
fun ImageAsset.assertPixels(
    expectedSize: IntSize? = null,
    expectedColorProvider: (pos: IntOffset) -> Color?
) = asAndroidBitmap().assertPixels(expectedSize, expectedColorProvider)

/**
 * Asserts that the color at a specific pixel in the bitmap at ([x], [y]) is [expected].
 */
@Suppress("DEPRECATION")
fun ImageAsset.assertPixelColor(
    expected: Color,
    x: Int,
    y: Int,
    error: (Color) -> String = { color -> "Pixel($x, $y) expected to be $expected, but was $color" }
) = asAndroidBitmap().assertPixelColor(expected, x, y, error)

/**
 * Asserts that the expected color is present in this bitmap.
 *
 * @throws AssertionError if the expected color is not present.
 */
@Suppress("DEPRECATION")
fun ImageAsset.assertContainsColor(
    expectedColor: Color
) = asAndroidBitmap().assertContainsColor(expectedColor).asImageAsset()

/**
 * Tests to see if the given point is within the path. (That is, whether the
 * point would be in the visible portion of the path if the path was used
 * with [Canvas.clipPath].)
 *
 * The `point` argument is interpreted as an offset from the origin.
 *
 * Returns true if the point is in the path, and false otherwise.
 */
@Suppress("DEPRECATION")
fun Path.contains(offset: Offset): Boolean = contains(offset)

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
@Suppress("DEPRECATION")
fun ImageAsset.assertShape(
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
) = asAndroidBitmap().assertShape(
    density,
    shape,
    shapeColor,
    backgroundColor,
    backgroundShape,
    sizeX,
    sizeY,
    shapeSizeX,
    shapeSizeY,
    centerX,
    centerY,
    shapeOverlapPixelCount
)

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
@Suppress("DEPRECATION")
fun ImageAsset.assertShape(
    density: Density,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    backgroundColor: Color,
    shapeColor: Color,
    shape: Shape = RectangleShape,
    shapeOverlapPixelCount: Float = 1.0f
) = asAndroidBitmap().assertShape(
    density,
    horizontalPadding,
    verticalPadding,
    backgroundColor,
    shapeColor,
    shape,
    shapeOverlapPixelCount
)
