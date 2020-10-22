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

package androidx.compose.ui.test

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.platform.AndroidOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.android.captureRegionToBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.DialogWindowProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.math.roundToInt

/**
 * Captures the underlying semantics node's surface into bitmap.
 *
 * This has a limitation that if there is another window covering part of this node, such a
 * window won't occur in this bitmap.
 *
 * @throws IllegalArgumentException if a bitmap is taken inside of a popup.
*/
@RequiresApi(Build.VERSION_CODES.O)
fun SemanticsNodeInteraction.captureToBitmap(): Bitmap {
    val node = fetchSemanticsNode("Failed to capture a node to bitmap.")
    // TODO(pavlis): Consider doing assertIsDisplayed here. Will need to move things around.
    var windowToUse: Window? = null

    // Validate we are not in popup
    val popupParentMaybe = node.findClosestParentNode(includeSelf = true) {
        it.config.contains(SemanticsProperties.IsPopup)
    }
    if (popupParentMaybe != null) {
        // We do not support capturing popups to bitmap
        throw IllegalArgumentException(
            "The node that is being captured to bitmap is in " +
                "a popup or is a popup itself. Popups currently cannot be captured to bitmap."
        )
    }

    @OptIn(ExperimentalLayoutNodeApi::class)
    val view = (node.componentNode.owner as AndroidOwner).view

    // If we are in dialog use its window to capture the bitmap
    val dialogParentNodeMaybe = node.findClosestParentNode(includeSelf = true) {
        it.config.contains(SemanticsProperties.IsDialog)
    }
    if (dialogParentNodeMaybe != null) {
        val dialogProvider = findDialogWindowProviderInParent(view)
            ?: throw IllegalArgumentException(
                "Could not find a dialog window provider to capture" +
                    " its bitmap"
            )
        windowToUse = dialogProvider.window

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // b/163023027
            throw IllegalArgumentException("Cannot currently capture dialogs on API lower than 28!")
        }
    }

    val nodeBounds = node.boundsInRoot
    val nodeBoundsRect = android.graphics.Rect(
        nodeBounds.left.roundToInt(),
        nodeBounds.top.roundToInt(),
        nodeBounds.right.roundToInt(),
        nodeBounds.bottom.roundToInt()
    )

    val locationInWindow = intArrayOf(0, 0)
    view.getLocationInWindow(locationInWindow)
    val x = locationInWindow[0]
    val y = locationInWindow[1]

    // Now these are bounds in window
    nodeBoundsRect.offset(x, y)

    return captureRegionToBitmap(nodeBoundsRect, view, windowToUse)
}

private fun findDialogWindowProviderInParent(view: View): DialogWindowProvider? {
    if (view is DialogWindowProvider) {
        return view
    }
    val parent = view.parent ?: return null
    if (parent is View) {
        return findDialogWindowProviderInParent(parent)
    }
    return null
}

/**
 * Captures the underlying view's surface into bitmap.
 *
 * This has currently several limitations. Currently we assume that the view is hosted in
 * Activity's window. Also if there is another window covering part of the component if won't occur
 * in the bitmap as this is taken from the component's window surface.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun View.captureToBitmap(): Bitmap {
    val locationInWindow = intArrayOf(0, 0)
    getLocationInWindow(locationInWindow)
    val x = locationInWindow[0]
    val y = locationInWindow[1]
    val boundsInWindow = android.graphics.Rect(x, y, x + width, y + height)
    return captureRegionToBitmap(boundsInWindow, this)
}

/**
 * A helper function to run asserts on [Bitmap].
 *
 * @param expectedSize The expected size of the bitmap. Leave null to skip the check.
 * @param expectedColorProvider Returns the expected color for the provided pixel position.
 * The returned color is then asserted as the expected one on the given bitmap.
 *
 * @throws AssertionError if size or colors don't match.
 */
fun Bitmap.assertPixels(
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

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pxPos = IntOffset(x, y)
            val expectedClr = expectedColorProvider(pxPos)
            if (expectedClr != null) {
                assertPixelColor(expectedClr, x, y)
            }
        }
    }
}

/**
 * Asserts that the color at a specific pixel in the bitmap at ([x], [y]) is [expected].
 */
fun Bitmap.assertPixelColor(
    expected: Color,
    x: Int,
    y: Int,
    error: (Color) -> String = { color -> "Pixel($x, $y) expected to be $expected, but was $color" }
) {
    val color = Color(getPixel(x, y))
    val errorString = error(color)
    assertEquals(errorString, expected.red, color.red, 0.02f)
    assertEquals(errorString, expected.green, color.green, 0.02f)
    assertEquals(errorString, expected.blue, color.blue, 0.02f)
    assertEquals(errorString, expected.alpha, color.alpha, 0.02f)
}

/**
 * Asserts that the expected color is present in this bitmap.
 *
 * @throws AssertionError if the expected color is not present.
 */
fun Bitmap.assertContainsColor(
    expectedColor: Color
): Bitmap {
    if (!containsColor(expectedColor)) {
        throw AssertionError("The given color $expectedColor was not found in the bitmap.")
    }
    return this
}

private fun Bitmap.containsColor(expectedColor: Color): Boolean {
    val pixels = IntArray(width * height).apply {
        getPixels(this, 0, width, 0, 0, width, height)
    }
    for (x in 0 until width) {
        for (y in 0 until height) {
            val color = Color(pixels[x + y * width])
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
fun Bitmap.assertShape(
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
    assertTrue(centerX + sizeX / 2 <= width)
    assertTrue(centerX - sizeX / 2 >= 0.0f)
    assertTrue(centerY + sizeY / 2 <= height)
    assertTrue(centerY - sizeY / 2 >= 0.0f)
    val outline = shape.createOutline(Size(shapeSizeX, shapeSizeY), density)
    val path = Path()
    path.addOutline(outline)
    val shapeOffset = Offset(
        (centerX - shapeSizeX / 2f),
        (centerY - shapeSizeY / 2f)
    )
    val backgroundPath = Path()
    backgroundPath.addOutline(backgroundShape.createOutline(Size(sizeX, sizeY), density))
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
                assertPixelColor(shapeColor, x, y)
            } else if (isOutside) {
                assertPixelColor(backgroundColor, x, y)
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
fun Bitmap.assertShape(
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

private fun pixelCloserToCenter(offset: Offset, shapeSizeX: Float, shapeSizeY: Float, delta: Float):
    Offset {
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