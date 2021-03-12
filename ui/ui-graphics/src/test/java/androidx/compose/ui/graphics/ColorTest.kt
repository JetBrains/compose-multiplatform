/*
 * Copyright (C) 2019 The Android Open Source Project
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

import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.util.lerp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ColorTest {
    private val adobeColorSpace = ColorSpaces.AdobeRgb
    private val srgbColor = Color(0xFFFF8000)
    private val adobeColor = Color(
        red = 0.8916f, green = 0.4980f, blue = 0.1168f,
        colorSpace = ColorSpaces.AdobeRgb
    )
    private val epsilon = 0.0005f // Float16 squished into ColorLong isn't very accurate.

    @Test
    fun colorSpace() {
        assertEquals(ColorSpaces.Srgb, srgbColor.colorSpace)
        assertEquals(ColorSpaces.AdobeRgb, adobeColor.colorSpace)
    }

    @Test
    fun convert() {
        val targetColor = srgbColor.convert(adobeColorSpace)

        assertEquals(adobeColor.colorSpace, targetColor.colorSpace)
        assertEquals(adobeColor.red, targetColor.red, epsilon)
        assertEquals(adobeColor.green, targetColor.green, epsilon)
        assertEquals(adobeColor.blue, targetColor.blue, epsilon)
        assertEquals(adobeColor.alpha, targetColor.alpha, epsilon)
    }

    @Test
    fun toArgb_fromSrgb() {
        assertEquals(0xFFFF8000.toInt(), srgbColor.toArgb())
    }

    @Test
    fun toArgb_fromAdobeRgb() {
        assertEquals(0xFFFF8000.toInt(), adobeColor.toArgb())
    }

    @Test
    fun red() {
        assertEquals(1f, srgbColor.red, 0f)
        assertEquals(0.8916f, adobeColor.red, epsilon)
    }

    @Test
    fun green() {
        assertEquals(0.5019608f, srgbColor.green, epsilon)
        assertEquals(0.4980f, adobeColor.green, epsilon)
    }

    @Test
    fun blue() {
        assertEquals(0f, srgbColor.blue, 0f)
        assertEquals(0.1168f, adobeColor.blue, epsilon)
    }

    @Test
    fun alpha() {
        assertEquals(1f, srgbColor.alpha, 0f)
        assertEquals(1f, adobeColor.alpha, 0f)
    }

    @Test
    fun luminance() {
        assertEquals(0f, Color.Black.luminance(), 0f)
        assertEquals(0.0722f, Color.Blue.luminance(), epsilon)
        assertEquals(0.2126f, Color.Red.luminance(), epsilon)
        assertEquals(0.7152f, Color.Green.luminance(), epsilon)
        assertEquals(1f, Color.White.luminance(), 0f)
    }

    @Test
    fun testToString() {
        assertEquals("Color(1.0, 0.5019608, 0.0, 1.0, sRGB IEC61966-2.1)", srgbColor.toString())
    }

    @Test
    fun lerp() {
        val red = Color.Red
        val green = Color.Green

        val redOklab = red.convert(ColorSpaces.Oklab)
        val greenOklab = green.convert(ColorSpaces.Oklab)

        for (i in 0..255) {
            val t = i / 255f
            val color = lerp(red, green, t)
            val expectedOklab = Color(
                red = lerp(redOklab.red, greenOklab.red, t),
                green = lerp(
                    redOklab.green,
                    greenOklab.green,
                    t
                ),
                blue = lerp(
                    redOklab.blue,
                    greenOklab.blue,
                    t
                ),
                colorSpace = ColorSpaces.Oklab
            )
            val expected = expectedOklab.convert(ColorSpaces.Srgb)
            val colorARGB = Color(color.toArgb())
            val expectedARGB = Color(expected.toArgb())
            assertEquals(
                "at t = $t[$i] was ${colorARGB.toArgb().toHexString()}, " +
                    "expecting ${expectedARGB.toArgb().toHexString()}",
                expectedARGB, colorARGB
            )
        }

        val transparentRed = Color.Red.copy(alpha = 0f)
        for (i in 0..255) {
            val t = i / 255f
            val color = lerp(red, transparentRed, t)
            val expected = Color.Red.copy(
                alpha = lerp(
                    1f,
                    0f,
                    t
                )
            )
            val colorARGB = Color(color.toArgb())
            val expectedARGB = Color(expected.toArgb())
            assertEquals(
                "at t = $t[$i] was ${colorARGB.toArgb().toHexString()}, " +
                    "expecting ${expectedARGB.toArgb().toHexString()}",
                expectedARGB, colorARGB
            )
        }
    }

    @Test
    fun compositeColorsSameColorSpace() {
        val background = Color(0x7f7f0000)
        val foreground = Color(0x7f007f00)
        val result = foreground.compositeOver(background)

        assertEquals(0.16f, result.red, 0.01f)
        assertEquals(0.33f, result.green, 0.01f)
        assertEquals(0.00f, result.blue, 0.01f)
        assertEquals(0.75f, result.alpha, 0.01f)
    }

    @Test
    fun compositeColorsDifferentColorSpace() {
        val background = Color(0.5f, 0.0f, 0.0f, 0.5f, ColorSpaces.DisplayP3)
        val foreground = Color(0x7f007f00)
        val result = foreground.compositeOver(background)

        assertEquals(ColorSpaces.DisplayP3, result.colorSpace)
        assertEquals(0.31f, result.red, 0.01f)
        assertEquals(0.33f, result.green, 0.01f)
        assertEquals(0.09f, result.blue, 0.01f)
        assertEquals(0.75f, result.alpha, 0.01f)
    }

    @Test
    fun compositeColorsLowAlpha() {
        val background = Color(1.0f, 0.0f, 0.0f, 0.01f)
        val foreground = Color(0.0f, 1.0f, 0.0f, 0.01f)
        val result = foreground.compositeOver(background)

        assertEquals(0.50f, result.red, 0.01f)
        assertEquals(0.50f, result.green, 0.01f)
        assertEquals(0.00f, result.blue, 0.01f)
        assertEquals(0.01f, result.alpha, 0.02f)
    }

    @Test
    fun compositeColorsZeroAlpha() {
        val background = Color(0x007f0000)
        val foreground = Color(0x00007f00)
        val result = foreground.compositeOver(background)

        assertEquals(Color(0f, 0f, 0f, 0f), result)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun srgbOutOfBounds_highRed() {
        Color(2f, 0f, 0f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun srgbOutOfBounds_lowRed() {
        Color(-1f, 0f, 0f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun srgbOutOfBounds_highGreen() {
        Color(0f, 2f, 0f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun srgbOutOfBounds_lowGreen() {
        Color(0f, -1f, 0f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun srgbOutOfBounds_highBlue() {
        Color(0f, 0f, 2f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun srgbOutOfBounds_lowBlue() {
        Color(0f, 0f, -1f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun srgbOutOfBounds_highAlpha() {
        Color(0f, 0f, 0f, 2f)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun srgbOutOfBounds_lowAlpha() {
        Color(0f, 0f, 0f, -1f)
    }

    @Test
    fun noneColor() {
        assertTrue(Color.Unspecified.isUnspecified)
        assertFalse(Color.Unspecified.isSpecified)
        assertFalse(Color.Red.isUnspecified)
        assertTrue(Color.Red.isSpecified)
        assertEquals(Color.Transparent.toArgb(), Color.Unspecified.toArgb())
        assertNotEquals(Color.Transparent, Color.Unspecified)
    }

    @Test
    fun testDestructuring() {
        val c = Color(0.15f, 0.2f, 0.3f, 0.5f, ColorSpaces.DisplayP3)
        val (red, green, blue, alpha, colorSpace) = c

        assertEquals(0.15f, red, epsilon)
        assertEquals(0.2f, green, epsilon)
        assertEquals(0.3f, blue, epsilon)
        assertEquals(0.5f, alpha, epsilon)
        assertEquals(ColorSpaces.DisplayP3, colorSpace)
    }

    @Test
    fun testDestructuringSubset() {
        val color = Color(0.2f, 0.3f, 0.4f, 0.6f, ColorSpaces.Aces)
        val (red, green, blue) = color

        assertEquals(0.2f, red, epsilon)
        assertEquals(0.3f, green, epsilon)
        assertEquals(0.4f, blue, epsilon)
    }

    @Test
    fun testDestructuringMiddleSubset() {
        val color = Color(0.2f, 0.3f, 0.4f, 0.6f, ColorSpaces.Aces)
        val (_, green, blue, alpha) = color

        assertEquals(0.3f, green, epsilon)
        assertEquals(0.4f, blue, epsilon)
        assertEquals(0.6f, alpha, epsilon)
    }

    companion object {
        @OptIn(kotlin.ExperimentalUnsignedTypes::class)
        fun Int.toHexString() = "0x${toUInt().toString(16).padStart(8, '0')}"
    }
}
