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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Rect
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class PixelMapTest {

    fun createImageAsset(): ImageAsset {
        val imageAsset = ImageAsset(100, 100)
        val canvas = Canvas(imageAsset)
        val paint = Paint().apply { this.color = Color.Red }

        canvas.drawRect(Rect(0.0f, 0.0f, 50.0f, 50.0f), paint)

        paint.color = Color.Blue
        canvas.drawRect(Rect(50.0f, 0.0f, 100.0f, 50.0f), paint)

        paint.color = Color.Green
        canvas.drawRect(Rect(0.0f, 50.0f, 50.0f, 100.0f), paint)

        paint.color = Color.Yellow
        canvas.drawRect(Rect(50.0f, 50.0f, 100.0f, 100.0f), paint)
        return imageAsset
    }

    @Test
    fun testImageAssetPixelMap() {
        val imageAsset = createImageAsset()
        val pixelmap = imageAsset.toPixelMap()

        Assert.assertEquals(Color.Red, pixelmap[0, 0])
        Assert.assertEquals(Color.Red, pixelmap[49, 0])
        Assert.assertEquals(Color.Red, pixelmap[0, 49])
        Assert.assertEquals(Color.Red, pixelmap[49, 49])

        Assert.assertEquals(Color.Blue, pixelmap[50, 0])
        Assert.assertEquals(Color.Blue, pixelmap[99, 0])
        Assert.assertEquals(Color.Blue, pixelmap[50, 49])
        Assert.assertEquals(Color.Blue, pixelmap[99, 49])

        Assert.assertEquals(Color.Green, pixelmap[0, 50])
        Assert.assertEquals(Color.Green, pixelmap[49, 50])
        Assert.assertEquals(Color.Green, pixelmap[0, 99])
        Assert.assertEquals(Color.Green, pixelmap[49, 99])

        Assert.assertEquals(Color.Yellow, pixelmap[50, 50])
        Assert.assertEquals(Color.Yellow, pixelmap[99, 50])
        Assert.assertEquals(Color.Yellow, pixelmap[50, 99])
        Assert.assertEquals(Color.Yellow, pixelmap[99, 99])
    }

    @Test
    fun testImageAssetSubsection() {
        val asset = createImageAsset()
        val subsectionWidth = 3
        val subsectionHeight = 2
        val bufferOffset = 3
        val pixelmap = asset.toPixelMap(
            startX = 48,
            startY = 49,
            stride = 3,
            width = 3,
            height = 2,
            buffer = IntArray(subsectionWidth * subsectionHeight + bufferOffset),
            bufferOffset = bufferOffset
        )

        Assert.assertEquals(Color.Red, pixelmap[1, 0])
        Assert.assertEquals(Color.Blue, pixelmap[2, 0])
        Assert.assertEquals(Color.Green, pixelmap[1, 1])
        Assert.assertEquals(Color.Yellow, pixelmap[2, 1])
    }
}