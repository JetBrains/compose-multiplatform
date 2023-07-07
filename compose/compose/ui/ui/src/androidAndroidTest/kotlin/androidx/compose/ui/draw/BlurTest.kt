/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlurTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @SmallTest
    fun testNoopBlur() {
        // If we are blurring with a 0 pixel radius and we are not clipping
        // the blurred result, this should return the default Modifier
        assertEquals(
            Modifier.blur(0.dp, BlurredEdgeTreatment.Unbounded),
            Modifier
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    private fun testBlurEdgeTreatment(
        blurredEdgeTreatment: BlurredEdgeTreatment,
        shape: Shape,
        clip: Boolean,
        tileMode: TileMode
    ) {
        val blurTag = "blurTag"
        val graphicsLayerTag = "graphicsLayerTag"
        rule.setContent {
            val density = LocalDensity.current.density
            val size = (100 / density).dp
            val blurRadius = (4 / density).dp
            val padding = (2 / density).dp
            val drawBlock: DrawScope.() -> Unit = {
                val blurRadiusPx = blurRadius.toPx()
                inset(blurRadiusPx, blurRadiusPx) {
                    drawRect(Color.Blue)
                }
            }
            val boxModifierChain = Modifier.size(size).background(Color.Black).padding(padding)
            Row {
                // Compare usages of Modifier.blur vs configuring the RenderEffect on a
                // graphicsLayer directly. The provided BlurredEdgeTreatment usage on Modifier.blur
                // should match the corresponding clip and TileMode configuration on the
                // usage of graphicsLayer
                Box(
                    Modifier.testTag(blurTag)
                        .then(boxModifierChain)
                        .blur(blurRadius, blurredEdgeTreatment)
                        .drawBehind(drawBlock)
                )
                Box(
                    Modifier.testTag(graphicsLayerTag)
                        .then(boxModifierChain)
                        .graphicsLayer {
                            val blurRadiusPx = blurRadius.toPx()
                            this.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, tileMode)
                            this.clip = clip
                            this.shape = shape
                        }
                        .drawBehind(drawBlock)
                )
            }
        }

        var blurBuffer: IntArray
        var graphicsLayerBuffer: IntArray

        val blurPixelMap = rule.onNodeWithTag(blurTag).captureToImage().apply {
            blurBuffer = IntArray(width * height)
            toPixelMap(buffer = blurBuffer)
        }
        val graphicsLayerMap = rule.onNodeWithTag(graphicsLayerTag).captureToImage().apply {
            graphicsLayerBuffer = IntArray(width * height)
            toPixelMap(buffer = graphicsLayerBuffer)
        }

        assertEquals(blurPixelMap.width, graphicsLayerMap.width)
        assertEquals(blurPixelMap.height, graphicsLayerMap.height)

        MSSIMMatcher(threshold = 0.99).compareBitmaps(
            blurBuffer,
            graphicsLayerBuffer,
            blurPixelMap.width,
            blurPixelMap.height
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    @MediumTest
    fun testRectBoundedBlur() {
        // Any bounded edge treatment should clip the underlying graphicsLayer and use
        // TileMode.Clamp in the corresponding BlurEffect
        testBlurEdgeTreatment(
            BlurredEdgeTreatment.Rectangle,
            RectangleShape,
            true,
            TileMode.Clamp
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    @MediumTest
    fun testUnboundedBlur() {
        // Any unbounded edge treatment should not clip the underlying graphicsLayer and use
        // TileMode.Decal in the corresponding BlurEffect
        testBlurEdgeTreatment(
            BlurredEdgeTreatment.Unbounded,
            RectangleShape,
            false,
            TileMode.Decal
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    @MediumTest
    fun testCircleBoundedBlur() {
        testBlurEdgeTreatment(
            BlurredEdgeTreatment(CircleShape),
            CircleShape,
            true,
            TileMode.Clamp
        )
    }

    @Test
    @SmallTest
    fun testRectangleBlurredEdgeTreatmentHasShape() {
        assertNotNull(BlurredEdgeTreatment.Rectangle.shape)
    }

    @Test
    @SmallTest
    fun testUnboundedBlurredEdgeTreatmentDoesNotHaveShape() {
        assertNull(BlurredEdgeTreatment.Unbounded.shape)
    }
}