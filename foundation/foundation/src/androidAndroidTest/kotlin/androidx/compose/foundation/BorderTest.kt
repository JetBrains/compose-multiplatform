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
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertShape
import androidx.compose.ui.test.captureToBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class BorderTest(val shape: Shape) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initShapes(): Array<Any> = arrayOf(
            RectangleShape, CircleShape, RoundedCornerShape(5.0f)
        )
    }

    @get:Rule
    val rule = createComposeRule()

    val testTag = "BorderParent"

    @Test
    fun border_color() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.preferredSize(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(10.0f.toDp(), Color.Red), shape)

                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToBitmap()
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
                    Modifier.preferredSize(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(
                            BorderStroke(10.0f.toDp(), SolidColor(Color.Red)),
                            shape
                        )
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToBitmap()
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
                    Modifier.preferredSize(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(1500.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToBitmap()
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
                    Modifier.preferredSize(40.0f.toDp(), 40.0f.toDp())
                        .background(color = Color.Blue)
                        .border(BorderStroke(-5.0f.toDp(), Color.Red), shape)
                ) {}
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToBitmap()
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
                    Modifier.preferredSize(40.0f.toDp(), 40.0f.toDp()).background(Color.White)
                ) {
                    Box(
                        Modifier.preferredSize(0.0f.toDp(), 40.0f.toDp())
                            .border(BorderStroke(4.0f.toDp(), Color.Red), shape)
                    ) {}
                }
            }
        }
        val bitmap = rule.onNodeWithTag(testTag).captureToBitmap()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.White,
            shapeColor = Color.White,
            shape = RectangleShape,
            shapeOverlapPixelCount = 1.0f
        )
    }

    @Composable
    fun SemanticParent(children: @Composable Density.() -> Unit) {
        Box {
            Box(modifier = Modifier.testTag(testTag)) {
                DensityAmbient.current.children()
            }
        }
    }
}