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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.floor
import kotlin.math.roundToInt

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class BorderTest(val shape: Shape) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initShapes(): Array<Any> = arrayOf(
            namedShape("Rectangle", RectangleShape),
            namedShape("Circle", CircleShape),
            namedShape("Rounded", RoundedCornerShape(5.0f))
        )

        private fun namedShape(name: String, shape: Shape): Shape = object : Shape by shape {
            override fun toString(): String = name
        }
    }

    @get:Rule
    val rule = createComposeRule()

    val testTag = "BorderParent"

    private val rtlAwareShape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            Outline.Rectangle(Rect(0f, 1f, 0f, 1f))
        } else {
            shape.createOutline(size, layoutDirection, density)
        }
    }

    @Test
    fun border_color() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(10.0f.toDp(), Color.Red), shape)

                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.Blue,
            shapeOverlapPixelCount = 3.0f
        )
    }

    @Test
    fun border_brush() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(
                            BorderStroke(10.0f.toDp(), SolidColor(Color.Red)),
                            shape
                        )
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.Blue,
            shapeOverlapPixelCount = 3.0f
        )
    }

    @Test
    fun border_biggerThanLayout_fills() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(1500.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.White,
            shapeColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun border_lessThanZero_doesNothing() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(-5.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.White,
            shapeColor = Color.Blue,
            shape = shape,
            backgroundShape = shape,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun border_zeroSizeLayout_drawsNothing() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp()).background(Color.White)
                ) {
                    Box(
                        Modifier.size(0.0f.toDp(), 40.0f.toDp())
                            .border(BorderStroke(4.0f.toDp(), Color.Red), shape)
                    ) {}
                }
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.White,
            shapeColor = Color.White,
            shape = RectangleShape,
            shapeOverlapPixelCount = 1.0f
        )
    }

    @Test
    fun border_triangle_shape() {
        val testTag = "testTag"
        val triangle = GenericShape { size, _ ->
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            close()
        }
        rule.setContent {
            Box(
                Modifier.testTag(testTag)
                    .requiredSize(100.dp, 100.dp)
                    .background(Color.White)
                    .border(BorderStroke(10.dp, Color.Red), triangle)
            )
        }

        val offsetLeft = 30
        val offsetRight = 15
        val offsetTop = 15
        val offsetBottom = 30

        rule.onNodeWithTag(testTag).captureToImage().apply {
            val map = toPixelMap()
            assertEquals(Color.Red, map[offsetLeft, offsetTop]) // Top left
            assertEquals(Color.Red, map[width - offsetRight, offsetTop]) // Top right
            assertEquals(Color.Red, map[width - offsetRight, height - offsetBottom]) // Bottom right
            // inside triangle
            assertEquals(Color.White, map[floor(width * 3f / 4f).toInt(), height / 2])
        }
    }

    @Test
    fun border_non_simple_rounded_rect() {
        val topleft = 0f
        val topRight = 5f
        val bottomRight = 10f
        val bottomLeft = 15f
        val roundRectTag = "roundRectTag"
        var borderPx = 0f
        rule.setContent {
            val size = 50.dp
            val border = 10.dp

            with(LocalDensity.current) {
                borderPx = border.toPx()
            }
            Box(
                Modifier.testTag(roundRectTag)
                    .size(size)
                    .background(Color.White)
                    .border(
                        BorderStroke(border, Color.Red),
                        RoundedCornerShape(
                            topStart = topleft,
                            topEnd = topRight,
                            bottomStart = bottomLeft,
                            bottomEnd = bottomRight
                        )
                    )
            )
        }

        rule.onNodeWithTag(roundRectTag).captureToImage().apply {
            val map = toPixelMap()
            val offset = 2
            assertEquals(Color.Red, map[offset, offset])
            assertEquals(Color.White, map[width - 1, 0])
            assertEquals(Color.White, map[width - 1, height - 1])
            assertEquals(Color.White, map[0, height - 1])

            assertEquals(Color.White, map[borderPx.toInt() + offset, borderPx.toInt() + offset])
            assertEquals(
                Color.White,
                map[
                    map.width - borderPx.toInt() - offset,
                    borderPx.toInt() + offset
                ]
            )
            assertEquals(
                Color.White,
                map[
                    map.width - borderPx.toInt() - offset,
                    map.height - borderPx.toInt() - offset
                ]
            )
            assertEquals(
                Color.White,
                map[
                    borderPx.toInt() + offset,
                    map.height - borderPx.toInt() - offset
                ]
            )

            val topRightOffset = (topRight / 2).roundToInt()
            assertEquals(Color.Red, map[width - 1 - topRightOffset, topRightOffset])

            val bottomRightOffset = (bottomRight / 2).roundToInt()
            assertEquals(Color.Red, map[width - 1 - bottomRightOffset, bottomRightOffset])

            val bottomLeftOffset = (bottomLeft / 2).roundToInt()
            assertEquals(Color.Red, map[bottomLeftOffset, height - 1 - bottomLeftOffset])
        }
    }

    @Test
    fun border_rtl_initially() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(10.0f.toDp(), Color.Red), rtlAwareShape)
                ) {}
            }
        }
        rule.onNodeWithTag(testTag).captureToImage().assertShape(
            density = rule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.Blue,
            shapeOverlapPixelCount = 3.0f
        )
    }

    @Test
    fun border_rtl_after_switch() {
        val direction = mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            SemanticParent {
                CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                    Box(
                        Modifier.size(40.0f.toDp(), 40.0f.toDp())
                            .background(color = Color.Blue)
                            .border(BorderStroke(10.0f.toDp(), Color.Red), rtlAwareShape)
                    ) {}
                }
            }
        }

        rule.runOnIdle {
            direction.value = LayoutDirection.Rtl
        }
        rule.onNodeWithTag(testTag).captureToImage().assertShape(
            density = rule.density,
            backgroundColor = Color.Red,
            shape = shape,
            backgroundShape = shape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.Blue,
            shapeOverlapPixelCount = 3.0f
        )
    }

    @Composable
    fun SemanticParent(content: @Composable Density.() -> Unit) {
        Box {
            Box(modifier = Modifier.testTag(testTag)) {
                LocalDensity.current.content()
            }
        }
    }
}