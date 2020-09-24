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
        val scale = ContentScale.None.scale(
            srcSize = Size(100.0f, 100.0f),
            dstSize = Size(200.0f, 200.0f)
        )
        assertEquals(1.0f, scale)
    }

    @Test
    fun testContentScaleFit() {
        val scale = ContentScale.Fit.scale(
            srcSize = Size(200.0f, 100.0f),
            dstSize = Size(100.0f, 200.0f)
        )
        assertEquals(.5f, scale)
    }

    @Test
    fun testContentScaleFillWidth() {
        val scale = ContentScale.FillWidth.scale(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(100.0f, 200.0f)
        )
        assertEquals(0.25f, scale)
    }

    @Test
    fun testScaleFillHeight() {
        val scale = ContentScale.FillHeight.scale(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(100.0f, 200.0f)
        )
        assertEquals(2.0f, scale)
    }

    @Test
    fun testContentScaleCrop() {
        val scale = ContentScale.Crop.scale(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(100.0f, 200.0f)
        )
        assertEquals(2.0f, scale)
    }

    @Test
    fun testContentScaleInside() {
        val scale = ContentScale.Inside.scale(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(100.0f, 200.0f)
        )
        assertEquals(0.25f, scale)
    }

    @Test
    fun testContentScaleInsideLargeDst() {
        // If the src is smaller than the destination, ensure no scaling is done
        val scale = ContentScale.Inside.scale(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(900.0f, 200.0f)
        )
        assertEquals(1.0f, scale)
    }

    @Test
    fun testContentFitInsideLargeDst() {
        val scale = ContentScale.Fit.scale(
            srcSize = Size(400.0f, 100.0f),
            dstSize = Size(900.0f, 200.0f)
        )
        assertEquals(2.0f, scale)
    }

    @Test
    fun testContentScaleCropWidth() {
        val scale = ContentScale.Crop.scale(
            srcSize = Size(100.0f, 400.0f),
            dstSize = Size(200.0f, 200.0f)
        )
        assertEquals(2.00f, scale)
    }

    @Test
    fun testContentScaleCropHeight() {
        val scale = ContentScale.Crop.scale(
            srcSize = Size(300.0f, 100.0f),
            dstSize = Size(200.0f, 200.0f)
        )
        assertEquals(2.00f, scale)
    }
}