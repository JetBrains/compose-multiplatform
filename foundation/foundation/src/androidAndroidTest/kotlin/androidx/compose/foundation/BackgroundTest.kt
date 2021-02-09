
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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class BackgroundTest {

    @get:Rule
    val rule = createComposeRule()

    private val contentTag = "Content"

    private val rtlAwareShape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            RectangleShape.createOutline(size, layoutDirection, density)
        } else {
            CircleShape.createOutline(size, layoutDirection, density)
        }
    }

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun background_colorRect() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40f.toDp()).background(Color.Magenta),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(20f.toDp()).background(Color.White))
                }
            }
        }
        val bitmap = rule.onNodeWithTag(contentTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Magenta,
            shape = RectangleShape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.White
        )
    }

    @Test
    fun background_brushRect() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40f.toDp()).background(Color.Magenta),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(20f.toDp())
                            .background(SolidColor(Color.White))
                    )
                }
            }
        }
        val bitmap = rule.onNodeWithTag(contentTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Magenta,
            shape = RectangleShape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.White
        )
    }

    @Test
    fun background_colorCircle() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40f.toDp())
                        .background(Color.Magenta)
                        .background(color = Color.White, shape = CircleShape)
                )
            }
        }
        val bitmap = rule.onNodeWithTag(contentTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun background_brushCircle() {
        rule.setContent {
            SemanticParent {
                Box(
                    Modifier.size(40f.toDp())
                        .background(Color.Magenta)
                        .background(
                            brush = SolidColor(Color.White),
                            shape = CircleShape
                        )
                )
            }
        }
        val bitmap = rule.onNodeWithTag(contentTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun background_rtl_initially() {
        rule.setContent {
            SemanticParent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        Modifier.size(40f.toDp())
                            .background(Color.Magenta)
                            .background(
                                brush = SolidColor(Color.White),
                                shape = rtlAwareShape
                            )
                    )
                }
            }
        }
        val bitmap = rule.onNodeWithTag(contentTag).captureToImage()
        bitmap.assertShape(
            density = rule.density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun background_rtl_after_switch() {
        val direction = mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            SemanticParent {
                CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                    Box(
                        Modifier.size(40f.toDp())
                            .background(Color.Magenta)
                            .background(
                                brush = SolidColor(Color.White),
                                shape = rtlAwareShape
                            )
                    )
                }
            }
        }

        rule.runOnIdle {
            direction.value = LayoutDirection.Rtl
        }
        rule.onNodeWithTag(contentTag).captureToImage().assertShape(
            density = rule.density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun testInspectableParameter1() {
        val modifier = Modifier.background(Color.Magenta) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("background")
        assertThat(modifier.valueOverride).isEqualTo(Color.Magenta)
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("color", Color.Magenta),
            ValueElement("shape", RectangleShape)
        )
    }

    @Test
    fun testInspectableParameter2() {
        val modifier = Modifier.background(SolidColor(Color.Red)) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("background")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("alpha", 1.0f),
            ValueElement("brush", SolidColor(Color.Red)),
            ValueElement("shape", RectangleShape)
        )
    }

    @Test
    fun testEquals() {
        assertThat(Modifier.background(SolidColor(Color.Red)))
            .isEqualTo(Modifier.background(SolidColor(Color.Red)))
    }

    @Composable
    private fun SemanticParent(content: @Composable Density.() -> Unit) {
        Box(Modifier.testTag(contentTag)) {
            LocalDensity.current.content()
        }
    }
}