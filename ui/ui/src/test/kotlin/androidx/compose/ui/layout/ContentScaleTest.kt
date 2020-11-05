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

package androidx.compose.ui.layout

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ContentScaleTest {

    @Test
    fun testScaleNone() {
        val scale = ContentScale.None.computeScaleFactor(
            srcSize = Size(100.0f, 100.0f),
            dstSize = Size(200.0f, 200.0f)
        )
        assertEquals(1.0f, scale.scaleX)
        assertEquals(1.0f, scale.scaleY)
    }

    @Test
    fun testContentScaleFit() {
        val scale = ContentScale.Fit.computeScaleFactor(
            srcSize = Size(200.0f, 100.0f),
            dstSize = Size(100.0f, 200.0f)
        )
        assertEquals(.5f, scale.scaleX)
        assertEquals(.5f, scale.scaleY)
    }

    @Test
    fun testContentScaleFillWidth() {
        val scale = ContentScale.FillWidth.computeScaleFactor(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(100.0f, 200.0f)
        )
        assertEquals(0.25f, scale.scaleX)
        assertEquals(0.25f, scale.scaleY)
    }

    @Test
    fun testScaleFillHeight() {
        val scale = ContentScale.FillHeight.computeScaleFactor(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(100.0f, 200.0f)
        )
        assertEquals(2.0f, scale.scaleX)
        assertEquals(2.0f, scale.scaleY)
    }

    @Test
    fun testContentScaleCrop() {
        val scale = ContentScale.Crop.computeScaleFactor(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(100.0f, 200.0f)
        )
        assertEquals(2.0f, scale.scaleX)
        assertEquals(2.0f, scale.scaleY)
    }

    @Test
    fun testContentScaleInside() {
        val scale = ContentScale.Inside.computeScaleFactor(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(100.0f, 200.0f)
        )
        assertEquals(0.25f, scale.scaleX)
        assertEquals(0.25f, scale.scaleY)
    }

    @Test
    fun testContentScaleInsideLargeDst() {
        // If the src is smaller than the destination, ensure no scaling is done
        val scale = ContentScale.Inside.computeScaleFactor(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(900.0f, 200.0f)
        )
        assertEquals(1.0f, scale.scaleX)
        assertEquals(1.0f, scale.scaleY)
    }

    @Test
    fun testContentFitInsideLargeDst() {
        val scale = ContentScale.Fit.computeScaleFactor(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(900.0f, 200.0f)
        )
        assertEquals(2.0f, scale.scaleX)
        assertEquals(2.0f, scale.scaleY)
    }

    @Test
    fun testContentScaleCropWidth() {
        val scale = ContentScale.Crop.computeScaleFactor(
            srcSize = Size(100.0f, 400.0f),
            dstSize = Size(200.0f, 200.0f)
        )
        assertEquals(2.00f, scale.scaleX)
        assertEquals(2.00f, scale.scaleY)
    }

    @Test
    fun testContentScaleCropHeight() {
        val scale = ContentScale.Crop.computeScaleFactor(
            srcSize = Size(300.0f, 100.0f),
            dstSize = Size(200.0f, 200.0f)
        )
        assertEquals(2.00f, scale.scaleX)
        assertEquals(2.00f, scale.scaleY)
    }

    @Test
    fun testContentScaleFillBoundsUp() {
        val scale = ContentScale.FillBounds.computeScaleFactor(
            srcSize = Size(100f, 100f),
            dstSize = Size(300f, 700f)
        )
        assertEquals(3.0f, scale.scaleX)
        assertEquals(7.0f, scale.scaleY)
    }
}