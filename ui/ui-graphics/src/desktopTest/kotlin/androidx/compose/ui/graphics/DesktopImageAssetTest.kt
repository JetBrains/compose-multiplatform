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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopImageAssetTest : DesktopGraphicsTest() {
    private val canvas: Canvas = initCanvas(widthPx = 16, heightPx = 16)

    @Test
    fun drawOnImageAssetAlpha8() {
        val asset = ImageAsset(width = 8, height = 8, config = ImageAssetConfig.Alpha8)
        val assetCanvas = Canvas(asset)
        assetCanvas.drawImage(
            imageFromResource("androidx/compose/desktop/test.png"),
            Offset(4f, 4f),
            Paint()
        )
        canvas.drawImage(asset, Offset(4f, 4f), Paint())

        screenshotRule.snap(surface)
    }

    @Test
    fun drawOnImageAssetDisplayP3() {
        val asset = ImageAsset(width = 8, height = 8, colorSpace = ColorSpaces.DisplayP3)
        val assetCanvas = Canvas(asset)
        assetCanvas.drawImage(
            imageFromResource("androidx/compose/desktop/test.png"),
            Offset(4f, 4f),
            Paint()
        )
        canvas.drawImage(asset, Offset(4f, 4f), Paint())

        screenshotRule.snap(surface)
    }

    @Test
    fun drawOnImageAsset() {
        val asset = ImageAsset(width = 8, height = 8)
        val assetCanvas = Canvas(asset)
        assetCanvas.drawImage(
            imageFromResource("androidx/compose/desktop/test.png"),
            Offset(4f, 4f),
            Paint()
        )
        canvas.drawImage(asset, Offset(4f, 4f), Paint())

        screenshotRule.snap(surface)
    }

    @Test(expected = RuntimeException::class)
    fun `cannot draw on loaded ImageAsset`() {
        val asset = imageFromResource("androidx/compose/desktop/test.png")
        Canvas(asset)
    }

    @Test
    fun `attributes of loaded asset`() {
        val asset = imageFromResource("androidx/compose/desktop/test.png")
        assertEquals(8, asset.width)
        assertEquals(8, asset.height)
        assertTrue(asset.hasAlpha)
        assertEquals(ImageAssetConfig.Argb8888, asset.config)
        assertEquals(ColorSpaces.Srgb, asset.colorSpace)
    }

    @Test
    fun `read pixels of loaded asset`() {
        val asset = imageFromResource("androidx/compose/desktop/test.png")

        val array = IntArray(5)
        asset.readPixels(array, startX = 0, startY = 0, width = 1, height = 1, bufferOffset = 0)
        assertThat(array.map(::toHexString)).isEqualTo(
            listOf(
                "ffff0000",
                "00000000",
                "00000000",
                "00000000",
                "00000000"
            )
        )

        asset.readPixels(array, startX = 3, startY = 3, width = 2, height = 2, bufferOffset = 1)
        assertThat(array.map(::toHexString)).isEqualTo(
            listOf(
                "ffff0000",
                "ffff00ff",
                "ffffff00",
                "ff000000",
                "80000000"
            )
        )
    }

    @Test
    fun `read pixels of loaded asset with different stride`() {
        val asset = imageFromResource("androidx/compose/desktop/test.png")

        val array = IntArray(6)
        asset.readPixels(array, startX = 3, startY = 3, width = 2, height = 2, stride = 3)
        assertThat(array.map(::toHexString)).isEqualTo(
            listOf(
                "ffff00ff",
                "ffffff00",
                "00000000",
                "ff000000",
                "80000000",
                "00000000"
            )
        )
    }

    private fun toHexString(num: Int) = "%08x".format(num)
}
