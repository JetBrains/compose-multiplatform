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

package androidx.ui.test

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.assertContainsColor
import androidx.compose.ui.test.assertPixelColor
import androidx.compose.ui.test.assertPixels
import androidx.compose.ui.test.assertShape
import androidx.compose.ui.test.captureToBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * @Deprecated Moved to androidx.compose.ui.test
 * @throws IllegalArgumentException if a bitmap is taken inside of a popup.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun SemanticsNodeInteraction.captureToBitmap() = captureToBitmap()

/**
 *  @Deprecated Moved to androidx.compose.ui.test
 *  @throws AssertionError if size or colors don't match.
 */
fun Bitmap.assertPixels(
    expectedSize: IntSize? = null,
    expectedColorProvider: (pos: IntOffset) -> Color?
) = assertPixels(expectedSize, expectedColorProvider)

/**
 *  @Deprecated Moved to androidx.compose.ui.test
 *  @throws AssertionError if size or colors don't match.
 */
fun Bitmap.assertPixelColor(
    expected: Color,
    x: Int,
    y: Int,
    error: (Color) -> String = { color -> "Pixel($x, $y) expected to be $expected, but was $color" }
) = assertPixelColor(expected, x, y, error)

/**
 *  @Deprecated Moved to androidx.compose.ui.test
 *  @throws AssertionError if the expected color is not present.
 *
 */
fun Bitmap.assertContainsColor(
    expectedColor: Color
) = assertContainsColor(expectedColor)

/** @Deprecated Moved to androidx.compose.ui.test */
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
) = assertShape(
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

/** @Deprecated Moved to androidx.compose.ui.test */
fun Bitmap.assertShape(
    density: Density,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    backgroundColor: Color,
    shapeColor: Color,
    shape: Shape = RectangleShape,
    shapeOverlapPixelCount: Float = 1.0f
) = assertShape(
    density,
    horizontalPadding,
    verticalPadding,
    backgroundColor,
    shapeColor,
    shape,
    shapeOverlapPixelCount
)
