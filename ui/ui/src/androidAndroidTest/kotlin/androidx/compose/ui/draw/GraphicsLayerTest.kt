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

package androidx.compose.ui.draw

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Padding
import androidx.compose.ui.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class GraphicsLayerTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testLayerBoundsPosition() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            FixedSize(
                30,
                Modifier.padding(10).graphicsLayer().onGloballyPositioned {
                    coords = it
                }
            ) { /* no-op */ }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            assertEquals(Offset(10f, 10f), layoutCoordinates.positionInRoot())
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(10f, 10f, 40f, 40f), bounds)
            val global = layoutCoordinates.boundsInWindow()
            val position = layoutCoordinates.positionInWindow()
            assertEquals(position.x, global.left)
            assertEquals(position.y, global.top)
            assertEquals(30f, global.width)
            assertEquals(30f, global.height)
        }
    }

    @Test
    fun testScale() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(scaleX = 2f, scaleY = 3f).onGloballyPositioned {
                        coords = it
                    }
                ) {
                }
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(5f, 0f, 25f, 30f), bounds)
            assertEquals(Offset(5f, 0f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testScaleConvenienceXY() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.scale(scaleX = 2f, scaleY = 3f).onGloballyPositioned {
                        coords = it
                    }
                ) {
                }
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(5f, 0f, 25f, 30f), bounds)
            assertEquals(Offset(5f, 0f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testScaleConvenienceUniform() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.scale(scale = 2f).onGloballyPositioned {
                        coords = it
                    }
                ) {
                }
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(5f, 5f, 25f, 25f), bounds)
            assertEquals(Offset(5f, 5f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testRotation() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(scaleY = 3f, rotationZ = 90f).onGloballyPositioned {
                        coords = it
                    }
                ) {
                }
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(0f, 10f, 30f, 20f), bounds)
            assertEquals(Offset(30f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testRotationConvenience() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.rotate(90f).onGloballyPositioned {
                        coords = it
                    }
                ) {
                }
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(10.0f, 10f, 20f, 20f), bounds)
            assertEquals(Offset(20f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testRotationPivot() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(
                        rotationZ = 90f,
                        transformOrigin = TransformOrigin(1.0f, 1.0f)
                    ).onGloballyPositioned {
                        coords = it
                    }
                )
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(20f, 10f, 30f, 20f), bounds)
            assertEquals(Offset(30f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testTranslationXY() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(
                    10,
                    Modifier.graphicsLayer(
                        translationX = 5.0f,
                        translationY = 8.0f
                    ).onGloballyPositioned {
                        coords = it
                    }
                )
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(15f, 18f, 25f, 28f), bounds)
            assertEquals(Offset(15f, 18f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testClip() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(10, Modifier.graphicsLayer(clip = true)) {
                    FixedSize(
                        10,
                        Modifier.graphicsLayer(scaleX = 2f).onGloballyPositioned {
                            coords = it
                        }
                    ) {
                    }
                }
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            assertEquals(Rect(10f, 10f, 20f, 20f), bounds)
            // Positions aren't clipped
            assertEquals(Offset(5f, 10f), layoutCoordinates.positionInRoot())
        }
    }

    @Test
    fun testSiblingComparisons() {
        var coords1: LayoutCoordinates? = null
        var coords2: LayoutCoordinates? = null
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.requiredSize(25.toDp())
                        .graphicsLayer(
                            rotationZ = 30f,
                            clip = true
                        )
                ) {
                    Box(
                        Modifier.graphicsLayer(
                            rotationZ = 90f,
                            transformOrigin = TransformOrigin(0f, 1f),
                            clip = true
                        )
                            .requiredSize(20.toDp(), 10.toDp())
                            .align(AbsoluteAlignment.TopLeft)
                            .onGloballyPositioned {
                                coords1 = it
                            }
                    )
                    Box(
                        Modifier
                            .graphicsLayer(
                                rotationZ = -90f,
                                transformOrigin = TransformOrigin(0f, 1f),
                                clip = true
                            )
                            .requiredSize(10.toDp())
                            .align(AbsoluteAlignment.BottomRight)
                            .onGloballyPositioned {
                                coords2 = it
                            }
                    )
                }
            }
        }

        rule.onRoot().apply {
            assertEquals(Offset(15f, 5f), coords2!!.localPositionOf(coords1!!, Offset.Zero))
            assertEquals(Offset(-5f, 5f), coords2!!.localPositionOf(coords1!!, Offset(20f, 0f)))
            assertEquals(
                Rect(-5f, -5f, 15f, 5f),
                coords2!!.localBoundingBoxOf(coords1!!, false)
            )
            assertEquals(
                Rect(0f, 0f, 10f, 5f),
                coords2!!.localBoundingBoxOf(coords1!!, true)
            )
        }
    }

    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCameraDistanceWithRotationY() {
        val testTag = "parent"
        rule.setContent {
            Box(modifier = Modifier.testTag(testTag).wrapContentSize()) {
                Box(
                    modifier = Modifier
                        .requiredSize(100.dp)
                        .background(Color.Gray)
                        .graphicsLayer(rotationY = 25f, cameraDistance = 1.0f)
                        .background(Color.Red)
                ) {
                    Box(modifier = Modifier.requiredSize(100.dp))
                }
            }
        }

        rule.onNodeWithTag(testTag).captureToImage().asAndroidBitmap().apply {
            assertEquals(Color.Red.toArgb(), getPixel(0, 0))
            assertEquals(Color.Red.toArgb(), getPixel(0, height - 1))
            assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 10, height / 2))
            assertEquals(Color.Gray.toArgb(), getPixel(width - 1 - 10, height / 2))
            assertEquals(Color.Gray.toArgb(), getPixel(width - 1, 0))
            assertEquals(Color.Gray.toArgb(), getPixel(width - 1, height - 1))
        }
    }

    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testEmptyClip() {
        val EmptyRectangle = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ) = Outline.Rectangle(Rect.Zero)
        }
        val tag = "testTag"
        rule.setContent {
            Box(modifier = Modifier.testTag(tag).requiredSize(100.dp).background(Color.Blue)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer(clip = true, shape = EmptyRectangle)
                        .background(Color.Red)
                )
            }
        }

        // Results should match background color of parent. Because the child Box is clipped to
        // an empty rectangle, no red pixels from its background should be visible
        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Blue }
    }

    @Test
    fun testTotalClip() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            Padding(10) {
                FixedSize(10, Modifier.graphicsLayer(clip = true)) {
                    FixedSize(
                        10,
                        Modifier.padding(20).onGloballyPositioned {
                            coords = it
                        }
                    ) {
                    }
                }
            }
        }

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            val bounds = layoutCoordinates.boundsInRoot()
            // should be completely clipped out
            assertEquals(0f, bounds.width)
            assertEquals(0f, bounds.height)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun invalidateWhenWeHaveSemanticModifierAfterLayer() {
        var color by mutableStateOf(Color.Red)
        rule.setContent {
            FixedSize(
                5,
                Modifier.graphicsLayer().testTag("tag").background(color)
            )
        }

        rule.runOnIdle {
            color = Color.Green
        }

        rule.onNodeWithTag("tag")
            .captureToImage()
            .assertPixels {
                color
            }
    }

    @Test
    fun testDpPixelConversions() {
        var density: Density? = null
        var testDpConversion = 0f
        var testFontScaleConversion = 0f
        rule.setContent {
            density = LocalDensity.current
            // Verify that the current density is passed to the graphics layer
            // implementation and that density dependent methods are consuming it
            Box(
                modifier = Modifier.graphicsLayer {
                    testDpConversion = 2.dp.toPx()
                    testFontScaleConversion = 3.dp.toSp().toPx()
                }
            )
        }

        rule.runOnIdle {
            with(density!!) {
                assertEquals(2.dp.toPx(), testDpConversion)
                assertEquals(3.dp.toSp().toPx(), testFontScaleConversion)
            }
        }
    }

    @Test
    fun testClickOnScaledElement() {
        var firstClicked = false
        var secondClicked = false
        rule.setContent {
            Layout(
                content = {
                    Box(
                        Modifier.fillMaxSize().clickable {
                            firstClicked = true
                        }
                    )
                    Box(
                        Modifier.fillMaxSize().clickable {
                            secondClicked = true
                        }
                    )
                },
                modifier = Modifier.testTag("layout")
            ) { measurables, _ ->
                val itemConstraints = Constraints.fixed(100, 100)
                val first = measurables[0].measure(itemConstraints)
                val second = measurables[1].measure(itemConstraints)
                layout(100, 200) {
                    val layer: GraphicsLayerScope.() -> Unit = {
                        scaleX = 0.5f
                        scaleY = 0.5f
                    }
                    first.placeWithLayer(0, 0, layerBlock = layer)
                    second.placeWithLayer(0, 100, layerBlock = layer)
                }
            }
        }

        rule.onNodeWithTag("layout")
            .performGesture {
                click(position = Offset(50f, 170f))
            }

        rule.runOnIdle {
            assertFalse("First element is clicked", firstClicked)
            assertTrue("Second element is not clicked", secondClicked)
        }
    }
}