
/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.assertShape
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.containsExactly
import androidx.compose.foundation.isNull
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class BackgroundTest {


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

    private val sceneSize = Size(40f, 40f)

    @BeforeTest
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @AfterTest
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun background_colorRect() = runSkikoComposeUiTest(sceneSize) {
        setContent {
            SemanticParent {
                Box(
                    Modifier.size(40f.toDp()).background(Color.Magenta),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(20f.toDp()).background(Color.White))
                }
            }
        }
        waitForIdle()
        val bitmap = captureToImage()

        bitmap.assertShape(
            density = density,
            backgroundColor = Color.Magenta,
            shape = RectangleShape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.White
        )
    }

    @Test
    fun background_brushRect() = runSkikoComposeUiTest(sceneSize) {
        setContent {
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
        waitForIdle()
        val bitmap = captureToImage()

        bitmap.assertShape(
            density = density,
            backgroundColor = Color.Magenta,
            shape = RectangleShape,
            shapeSizeX = 20.0f,
            shapeSizeY = 20.0f,
            shapeColor = Color.White
        )
    }

    @Test
    fun background_colorCircle() = runSkikoComposeUiTest(sceneSize) {
        setContent {
            SemanticParent {
                Box(
                    Modifier.size(40f.toDp())
                        .background(Color.Magenta)
                        .background(color = Color.White, shape = CircleShape)
                )
            }
        }
        waitForIdle()
        val bitmap = captureToImage()

        bitmap.assertShape(
            density = density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun background_brushCircle() = runSkikoComposeUiTest(sceneSize) {
        setContent {
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
        waitForIdle()
        val bitmap = captureToImage()

        bitmap.assertShape(
            density = density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun background_rtl_initially() = runSkikoComposeUiTest(sceneSize) {
        setContent {
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
        waitForIdle()
        val bitmap = captureToImage()

        bitmap.assertShape(
            density = density,
            backgroundColor = Color.Magenta,
            shape = CircleShape,
            shapeColor = Color.White,
            shapeOverlapPixelCount = 2.0f
        )
    }

    @Test
    fun background_rtl_after_switch() = runSkikoComposeUiTest(sceneSize) {
        val direction = mutableStateOf(LayoutDirection.Ltr)
        setContent {
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

        runOnIdle {
            direction.value = LayoutDirection.Rtl
        }

        val bitmap = captureToImage()

        bitmap.assertShape(
            density = density,
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
