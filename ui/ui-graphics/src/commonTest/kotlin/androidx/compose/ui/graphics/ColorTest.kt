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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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
                expectedARGB, colorARGB,
                "at t = $t[$i] was ${colorARGB.toArgb().toHexString()}, " +
                    "expecting ${expectedARGB.toArgb().toHexString()}"
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
                expectedARGB, colorARGB,
                "at t = $t[$i] was ${colorARGB.toArgb().toHexString()}, " +
                    "expecting ${expectedARGB.toArgb().toHexString()}",
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

    @Test
    fun srgbOutOfBounds_highRed() {
        assertFailsWith<IllegalArgumentException> {
            Color(2f, 0f, 0f)
        }
    }

    @Test
    fun srgbOutOfBounds_lowRed() {
        assertFailsWith<IllegalArgumentException> {
            Color(-1f, 0f, 0f)
        }
    }

    @Test
    fun srgbOutOfBounds_highGreen() {
        assertFailsWith<IllegalArgumentException> {
            Color(0f, 2f, 0f)
        }
    }

    @Test
    fun srgbOutOfBounds_lowGreen() {
        assertFailsWith<IllegalArgumentException> {
            Color(0f, -1f, 0f)
        }
    }

    @Test
    fun srgbOutOfBounds_highBlue() {
        assertFailsWith<IllegalArgumentException> {
            Color(0f, 0f, 2f)
        }
    }

    @Test
    fun srgbOutOfBounds_lowBlue() {
        assertFailsWith<IllegalArgumentException> {
            Color(0f, 0f, -1f)
        }
    }

    @Test
    fun srgbOutOfBounds_highAlpha() {
        assertFailsWith<IllegalArgumentException> {
            Color(0f, 0f, 0f, 2f)
        }
    }

    @Test
    fun srgbOutOfBounds_lowAlpha() {
        assertFailsWith<IllegalArgumentException> {
            Color(0f, 0f, 0f, -1f)
        }
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

    @OptIn(ExperimentalGraphicsApi::class)
    @Test
    fun testHsvInSrgb() {
        assertEquals(Color.Transparent, Color.hsv(0f, 0f, 0f, 0f))
        assertEquals(Color.Black, Color.hsv(0f, 0f, 0f))
        assertEquals(Color.Black, Color.hsv(120f, 0f, 0f))
        assertEquals(Color.Black, Color.hsv(120f, 1f, 0f))
        assertEquals(Color.White, Color.hsv(0f, 0f, 1f))
        assertEquals(Color.White, Color.hsv(120f, 0f, 1f))
        assertEquals(Color.White, Color.hsv(240f, 0f, 1f))
        val gray = Color(0xFF808080)
        assertEquals(gray, Color.hsv(0f, 0f, 0.5f))
        assertEquals(gray, Color.hsv(120f, 0f, 0.5f))
        assertEquals(gray, Color.hsv(240f, 0f, 0.5f))

        assertEquals(Color.Red, Color.hsv(0f, 1f, 1f))
        assertEquals(Color.Yellow, Color.hsv(60f, 1f, 1f))
        assertEquals(Color.Green, Color.hsv(120f, 1f, 1f))
        assertEquals(Color.Cyan, Color.hsv(180f, 1f, 1f))
        assertEquals(Color.Blue, Color.hsv(240f, 1f, 1f))
        assertEquals(Color.Magenta, Color.hsv(300f, 1f, 1f))
        assertEquals(Color.Red, Color.hsv(360f, 1f, 1f))
    }

    @OptIn(ExperimentalGraphicsApi::class)
    @Test
    fun testHsvInLinearSrgb() {
        val lrgb = ColorSpaces.LinearSrgb
        val srgb = ColorSpaces.Srgb
        assertEquals(Color.Black, Color.hsv(0f, 0f, 0f, 1f, lrgb).convert(srgb))
        assertEquals(Color.Black, Color.hsv(120f, 0f, 0f, 1f, lrgb).convert(srgb))
        assertEquals(Color.Black, Color.hsv(120f, 1f, 0f, 1f, lrgb).convert(srgb))
        assertEquals(Color.White, Color.hsv(0f, 0f, 1f, 1f, lrgb).convert(srgb))
        assertEquals(Color.White, Color.hsv(120f, 0f, 1f, 1f, lrgb).convert(srgb))
        assertEquals(Color.White, Color.hsv(240f, 0f, 1f, 1f, lrgb).convert(srgb))
        val gray = Color(0.5f, 0.5f, 0.5f, 1f, lrgb)
        assertEquals(gray, Color.hsv(0f, 0f, 0.5f, 1f, lrgb))
        assertEquals(gray, Color.hsv(120f, 0f, 0.5f, 1f, lrgb))
        assertEquals(gray, Color.hsv(240f, 0f, 0.5f, 1f, lrgb))

        assertEquals(Color(1f, 0f, 0f, 1f, lrgb), Color.hsv(0f, 1f, 1f, 1f, lrgb))
        assertEquals(Color(1f, 1f, 0f, 1f, lrgb), Color.hsv(60f, 1f, 1f, 1f, lrgb))
        assertEquals(Color(0f, 1f, 0f, 1f, lrgb), Color.hsv(120f, 1f, 1f, 1f, lrgb))
        assertEquals(Color(0f, 1f, 1f, 1f, lrgb), Color.hsv(180f, 1f, 1f, 1f, lrgb))
        assertEquals(Color(0f, 0f, 1f, 1f, lrgb), Color.hsv(240f, 1f, 1f, 1f, lrgb))
        assertEquals(Color(1f, 0f, 1f, 1f, lrgb), Color.hsv(300f, 1f, 1f, 1f, lrgb))
        assertEquals(Color(1f, 0f, 0f, 1f, lrgb), Color.hsv(360f, 1f, 1f, 1f, lrgb))
    }

    @OptIn(ExperimentalGraphicsApi::class)
    @Test
    fun testHslInSrgb() {
        assertEquals(Color.Transparent, Color.hsl(0f, 0f, 0f, 0f))
        assertEquals(Color.Black, Color.hsl(0f, 0f, 0f))
        assertEquals(Color.Black, Color.hsl(120f, 0f, 0f))
        assertEquals(Color.Black, Color.hsl(120f, 1f, 0f))
        assertEquals(Color.White, Color.hsl(0f, 0f, 1f))
        assertEquals(Color.White, Color.hsl(120f, 1f, 1f))
        assertEquals(Color.White, Color.hsl(240f, 0.5f, 1f))
        val gray = Color(0xFF808080)
        assertEquals(gray, Color.hsl(0f, 0f, 0.5f))
        assertEquals(gray, Color.hsl(120f, 0f, 0.5f))
        assertEquals(gray, Color.hsl(240f, 0f, 0.5f))

        assertEquals(Color.Red, Color.hsl(0f, 1f, 0.5f))
        assertEquals(Color.Yellow, Color.hsl(60f, 1f, 0.5f))
        assertEquals(Color.Green, Color.hsl(120f, 1f, 0.5f))
        assertEquals(Color.Cyan, Color.hsl(180f, 1f, 0.5f))
        assertEquals(Color.Blue, Color.hsl(240f, 1f, 0.5f))
        assertEquals(Color.Magenta, Color.hsl(300f, 1f, 0.5f))
        assertEquals(Color.Red, Color.hsl(360f, 1f, 0.5f))
    }

    @OptIn(ExperimentalGraphicsApi::class)
    @Test
    fun testHslInLinearSrgb() {
        val lrgb = ColorSpaces.LinearSrgb
        val srgb = ColorSpaces.Srgb
        assertEquals(Color.Black, Color.hsl(0f, 0f, 0f, 1f, lrgb).convert(srgb))
        assertEquals(Color.Black, Color.hsl(120f, 0f, 0f, 1f, lrgb).convert(srgb))
        assertEquals(Color.Black, Color.hsl(120f, 1f, 0f, 1f, lrgb).convert(srgb))
        assertEquals(Color.White, Color.hsl(0f, 0f, 1f, 1f, lrgb).convert(srgb))
        assertEquals(Color.White, Color.hsl(120f, 0f, 1f, 1f, lrgb).convert(srgb))
        assertEquals(Color.White, Color.hsl(240f, 0f, 1f, 1f, lrgb).convert(srgb))
        val gray = Color(0.5f, 0.5f, 0.5f, 1f, lrgb)
        assertEquals(gray, Color.hsl(0f, 0f, 0.5f, 1f, lrgb))
        assertEquals(gray, Color.hsl(120f, 0f, 0.5f, 1f, lrgb))
        assertEquals(gray, Color.hsl(240f, 0f, 0.5f, 1f, lrgb))

        assertEquals(Color(1f, 0f, 0f, 1f, lrgb), Color.hsl(0f, 1f, 0.5f, 1f, lrgb))
        assertEquals(Color(1f, 1f, 0f, 1f, lrgb), Color.hsl(60f, 1f, 0.5f, 1f, lrgb))
        assertEquals(Color(0f, 1f, 0f, 1f, lrgb), Color.hsl(120f, 1f, 0.5f, 1f, lrgb))
        assertEquals(Color(0f, 1f, 1f, 1f, lrgb), Color.hsl(180f, 1f, 0.5f, 1f, lrgb))
        assertEquals(Color(0f, 0f, 1f, 1f, lrgb), Color.hsl(240f, 1f, 0.5f, 1f, lrgb))
        assertEquals(Color(1f, 0f, 1f, 1f, lrgb), Color.hsl(300f, 1f, 0.5f, 1f, lrgb))
        assertEquals(Color(1f, 0f, 0f, 1f, lrgb), Color.hsl(360f, 1f, 0.5f, 1f, lrgb))
    }

    // Make sure lerp produces the correct values. This may change if we change lerp's algorithm.
    // This is essentially a test for the non-allocating conversion logic to make sure that it
    // hasn't changed any of the calculations.
    @Test
    fun lerpValues() {
        val expected = longArrayOf(
            0xffff0000, 0xffff0102, 0xfffe0203, 0xfffe0305, 0xfffe0507, 0xfffe0609, 0xfffe070a,
            0xfffd080c, 0xfffd090d, 0xfffd0b0f, 0xfffd0b10, 0xfffc0d11, 0xfffc0e13, 0xfffc0e13,
            0xfffc1015, 0xfffc1116, 0xfffb1117, 0xfffb1218, 0xfffb1319, 0xfffb1319, 0xfffa141a,
            0xfffa151b, 0xfffa151c, 0xfffa161d, 0xfff9171d, 0xfff9181e, 0xfff9181f, 0xfff91920,
            0xfff91920, 0xfff81a21, 0xfff81a22, 0xfff81b22, 0xfff81b23, 0xfff71c24, 0xfff71d24,
            0xfff71d25, 0xfff71d26, 0xfff71e26, 0xfff61e27, 0xfff61f27, 0xfff61f28, 0xfff62028,
            0xfff52029, 0xfff5212a, 0xfff5212a, 0xfff5222b, 0xfff4222b, 0xfff4222c, 0xfff4232c,
            0xfff4232d, 0xfff4232d, 0xfff3242e, 0xfff3242e, 0xfff3252f, 0xfff3252f, 0xfff22530,
            0xfff22630, 0xfff22631, 0xfff22631, 0xfff22732, 0xfff12732, 0xfff12833, 0xfff12833,
            0xfff12833, 0xfff02934, 0xfff02935, 0xfff02935, 0xfff02935, 0xffef2a36, 0xffef2a36,
            0xffef2b37, 0xffef2b37, 0xffef2b37, 0xffee2b38, 0xffee2c38, 0xffee2c39, 0xffee2c39,
            0xffed2c3a, 0xffed2d3a, 0xffed2d3a, 0xffed2d3b, 0xffed2e3b, 0xffec2e3b, 0xffec2e3c,
            0xffec2f3c, 0xffec2f3d, 0xffeb2f3d, 0xffeb2f3e, 0xffeb2f3e, 0xffeb303e, 0xffea303f,
            0xffea303f, 0xffea313f, 0xffea3140, 0xffea3140, 0xffe93141, 0xffe93141, 0xffe93241,
            0xffe93242, 0xffe83242, 0xffe83342, 0xffe83343, 0xffe83343, 0xffe73343, 0xffe73344,
            0xffe73444, 0xffe73444, 0xffe63445, 0xffe63445, 0xffe63546, 0xffe63546, 0xffe63546,
            0xffe53547, 0xffe53547, 0xffe53647, 0xffe53648, 0xffe43648, 0xffe43648, 0xffe43648,
            0xffe43749, 0xffe43749, 0xffe33749, 0xffe3374a, 0xffe3384a, 0xffe3384a, 0xffe2384b,
            0xffe2384b, 0xffe2384b, 0xffe2384c, 0xffe1394c, 0xffe1394c, 0xffe1394d, 0xffe1394d,
            0xffe1394d, 0xffe03a4e, 0xffe03a4e, 0xffe03a4e, 0xffe03a4f, 0xffdf3a4f, 0xffdf3b4f,
            0xffdf3b50, 0xffdf3b50, 0xffdf3b50, 0xffde3b50, 0xffde3b51, 0xffde3c51, 0xffde3c51,
            0xffdd3c52, 0xffdd3c52, 0xffdd3c52, 0xffdd3c53, 0xffdd3d53, 0xffdc3d53, 0xffdc3d53,
            0xffdc3d54, 0xffdc3d54, 0xffdb3d54, 0xffdb3d55, 0xffdb3e55, 0xffdb3e55, 0xffda3e55,
            0xffda3e56, 0xffda3e56, 0xffda3e56, 0xffda3f57, 0xffd93f57, 0xffd93f57, 0xffd93f57,
            0xffd93f58, 0xffd83f58, 0xffd84058, 0xffd84059, 0xffd84059, 0xffd84059, 0xffd74059,
            0xffd7405a, 0xffd7405a, 0xffd7415a, 0xffd6415b, 0xffd6415b, 0xffd6415b, 0xffd6415b,
            0xffd5415c, 0xffd5415c, 0xffd5425c, 0xffd5425c, 0xffd5425d, 0xffd4425d, 0xffd4425d,
            0xffd4425e, 0xffd4425e, 0xffd3425e, 0xffd3435e, 0xffd3435f, 0xffd3435f, 0xffd2435f,
            0xffd2435f, 0xffd24360, 0xffd24360, 0xffd24360, 0xffd14460, 0xffd14461, 0xffd14461,
            0xffd14461, 0xffd04461, 0xffd04462, 0xffd04462, 0xffd04462, 0xffcf4562, 0xffcf4563,
            0xffcf4563, 0xffcf4563, 0xffcf4564, 0xffce4564, 0xffce4564, 0xffce4564, 0xffce4565,
            0xffcd4665, 0xffcd4665, 0xffcd4665, 0xffcd4666, 0xffcc4666, 0xffcc4666, 0xffcc4666,
            0xffcc4667, 0xffcc4767, 0xffcb4767, 0xffcb4767, 0xffcb4768, 0xffcb4768, 0xffca4768,
            0xffca4768, 0xffca4769, 0xffca4769, 0xffca4769, 0xffc94769, 0xffc9486a, 0xffc9486a,
            0xffc9486a, 0xffc8486a, 0xffc8486a, 0xffc8486b, 0xffc8486b, 0xffc7486b, 0xffc7486c,
            0xffc7496c, 0xffc7496c, 0xffc7496c, 0xffc6496d, 0xffc6496d, 0xffc6496d, 0xffc6496d,
            0xffc5496d, 0xffc5496e, 0xffc5496e, 0xffc5496e, 0xffc54a6e, 0xffc44a6f, 0xffc44a6f,
            0xffc44a6f, 0xffc44a6f, 0xffc34a70, 0xffc34a70, 0xffc34a70, 0xffc34a70, 0xffc24a70,
            0xffc24a71, 0xffc24b71, 0xffc24b71, 0xffc24b71, 0xffc14b72, 0xffc14b72, 0xffc14b72,
            0xffc14b72, 0xffc04b73, 0xffc04b73, 0xffc04b73, 0xffc04b73, 0xffc04b74, 0xffbf4b74,
            0xffbf4c74, 0xffbf4c74, 0xffbf4c74, 0xffbe4c75, 0xffbe4c75, 0xffbe4c75, 0xffbe4c75,
            0xffbd4c76, 0xffbd4c76, 0xffbd4c76, 0xffbd4c76, 0xffbc4c77, 0xffbc4c77, 0xffbc4c77,
            0xffbc4d77, 0xffbc4d78, 0xffbb4d78, 0xffbb4d78, 0xffbb4d78, 0xffbb4d78, 0xffba4d79,
            0xffba4d79, 0xffba4d79, 0xffba4d79, 0xffb94d79, 0xffb94d7a, 0xffb94d7a, 0xffb94d7a,
            0xffb94e7a, 0xffb84e7b, 0xffb84e7b, 0xffb84e7b, 0xffb84e7b, 0xffb74e7b, 0xffb74e7c,
            0xffb74e7c, 0xffb74e7c, 0xffb74e7c, 0xffb64e7d, 0xffb64e7d, 0xffb64e7d, 0xffb64e7d,
            0xffb54e7e, 0xffb54f7e, 0xffb54e7e, 0xffb54f7e, 0xffb44f7e, 0xffb44f7f, 0xffb44f7f,
            0xffb44f7f, 0xffb44f7f, 0xffb34f80, 0xffb34f80, 0xffb34f80, 0xffb34f80, 0xffb24f80,
            0xffb24f81, 0xffb24f81, 0xffb24f81, 0xffb24f81, 0xffb14f81, 0xffb14f82, 0xffb15082,
            0xffb15082, 0xffb05082, 0xffb05083, 0xffb05083, 0xffb05083, 0xffb05083, 0xffaf5083,
            0xffaf5084, 0xffaf5084, 0xffae5084, 0xffae5084, 0xffae5084, 0xffae5085, 0xffae5085,
            0xffad5085, 0xffad5085, 0xffad5086, 0xffad5086, 0xffac5086, 0xffac5086, 0xffac5086,
            0xffac5187, 0xffab5087, 0xffab5187, 0xffab5187, 0xffab5187, 0xffab5188, 0xffaa5188,
            0xffaa5188, 0xffaa5188, 0xffaa5189, 0xffa95189, 0xffa95189, 0xffa95189, 0xffa95189,
            0xffa9518a, 0xffa8518a, 0xffa8518a, 0xffa8518a, 0xffa8518a, 0xffa7518b, 0xffa7518b,
            0xffa7518b, 0xffa7518b, 0xffa6518b, 0xffa6518c, 0xffa6518c, 0xffa6518c, 0xffa6528c,
            0xffa5528d, 0xffa5528d, 0xffa5528d, 0xffa5528d, 0xffa4528d, 0xffa4528e, 0xffa4528e,
            0xffa4528e, 0xffa4528e, 0xffa3528e, 0xffa3528f, 0xffa3528f, 0xffa2528f, 0xffa2528f,
            0xffa2528f, 0xffa25290, 0xffa25290, 0xffa15290, 0xffa15290, 0xffa15290, 0xffa15291,
            0xffa05291, 0xffa05291, 0xffa05291, 0xffa05291, 0xffa05292, 0xff9f5292, 0xff9f5292,
            0xff9f5292, 0xff9f5292, 0xff9e5293, 0xff9e5293, 0xff9e5293, 0xff9e5293, 0xff9d5293,
            0xff9d5294, 0xff9d5394, 0xff9d5294, 0xff9d5394, 0xff9c5395, 0xff9c5395, 0xff9c5395,
            0xff9c5395, 0xff9b5395, 0xff9b5396, 0xff9b5396, 0xff9b5396, 0xff9b5396, 0xff9a5396,
            0xff9a5397, 0xff9a5397, 0xff9a5397, 0xff995397, 0xff995397, 0xff995398, 0xff995398,
            0xff985398, 0xff985398, 0xff985398, 0xff985398, 0xff975399, 0xff975399, 0xff975399,
            0xff975399, 0xff97539a, 0xff96539a, 0xff96539a, 0xff96539a, 0xff96539a, 0xff95539b,
            0xff95539b, 0xff95539b, 0xff95539b, 0xff94539b, 0xff94539c, 0xff94539c, 0xff94539c,
            0xff94539c, 0xff93539c, 0xff93539d, 0xff93539d, 0xff93539d, 0xff92539d, 0xff92539d,
            0xff92539d, 0xff92539e, 0xff92539e, 0xff91539e, 0xff91539e, 0xff91539f, 0xff90539f,
            0xff90539f, 0xff90539f, 0xff90539f, 0xff9053a0, 0xff8f53a0, 0xff8f53a0, 0xff8f53a0,
            0xff8f53a0, 0xff8e53a1, 0xff8e53a1, 0xff8e53a1, 0xff8e53a1, 0xff8e53a1, 0xff8d53a1,
            0xff8d53a2, 0xff8d53a2, 0xff8d53a2, 0xff8c53a2, 0xff8c53a2, 0xff8c53a3, 0xff8c53a3,
            0xff8b53a3, 0xff8b53a3, 0xff8b53a4, 0xff8b53a4, 0xff8b53a4, 0xff8a53a4, 0xff8a53a4,
            0xff8a53a4, 0xff8a53a5, 0xff8953a5, 0xff8953a5, 0xff8953a5, 0xff8953a5, 0xff8853a6,
            0xff8853a6, 0xff8853a6, 0xff8853a6, 0xff8753a6, 0xff8753a7, 0xff8753a7, 0xff8753a7,
            0xff8753a7, 0xff8653a7, 0xff8653a8, 0xff8653a8, 0xff8653a8, 0xff8553a8, 0xff8553a8,
            0xff8553a8, 0xff8553a9, 0xff8453a9, 0xff8453a9, 0xff8453a9, 0xff8453a9, 0xff8353aa,
            0xff8353aa, 0xff8353aa, 0xff8353aa, 0xff8353ab, 0xff8252ab, 0xff8253ab, 0xff8253ab,
            0xff8252ab, 0xff8152ab, 0xff8152ac, 0xff8152ac, 0xff8152ac, 0xff8052ac, 0xff8052ac,
            0xff8052ad, 0xff8052ad, 0xff8052ad, 0xff7f52ad, 0xff7f52ad, 0xff7f52ae, 0xff7f52ae,
            0xff7e52ae, 0xff7e52ae, 0xff7e52ae, 0xff7e52ae, 0xff7d52af, 0xff7d52af, 0xff7d52af,
            0xff7d52af, 0xff7c52af, 0xff7c52b0, 0xff7c52b0, 0xff7c52b0, 0xff7c52b0, 0xff7b52b0,
            0xff7b52b1, 0xff7b52b1, 0xff7b52b1, 0xff7a52b1, 0xff7a52b1, 0xff7a52b2, 0xff7a52b2,
            0xff7952b2, 0xff7952b2, 0xff7952b2, 0xff7951b2, 0xff7851b3, 0xff7851b3, 0xff7851b3,
            0xff7851b3, 0xff7851b4, 0xff7751b4, 0xff7751b4, 0xff7751b4, 0xff7751b4, 0xff7651b4,
            0xff7651b5, 0xff7651b5, 0xff7651b5, 0xff7551b5, 0xff7551b5, 0xff7551b6, 0xff7551b6,
            0xff7451b6, 0xff7451b6, 0xff7451b6, 0xff7451b7, 0xff7451b7, 0xff7351b7, 0xff7351b7,
            0xff7351b7, 0xff7351b7, 0xff7251b8, 0xff7250b8, 0xff7250b8, 0xff7250b8, 0xff7150b8,
            0xff7150b9, 0xff7150b9, 0xff7150b9, 0xff7050b9, 0xff7050b9, 0xff7050b9, 0xff7050ba,
            0xff6f50ba, 0xff6f50ba, 0xff6f50ba, 0xff6f50ba, 0xff6f50bb, 0xff6e50bb, 0xff6e50bb,
            0xff6e50bb, 0xff6e50bb, 0xff6d50bc, 0xff6d50bc, 0xff6d4fbc, 0xff6d4fbc, 0xff6c4fbc,
            0xff6c4fbc, 0xff6c4fbd, 0xff6c4fbd, 0xff6b4fbd, 0xff6b4fbd, 0xff6b4fbe, 0xff6b4fbe,
            0xff6a4fbe, 0xff6a4fbe, 0xff6a4fbe, 0xff6a4fbe, 0xff694fbf, 0xff694fbf, 0xff694fbf,
            0xff694fbf, 0xff694fbf, 0xff684fc0, 0xff684ec0, 0xff684ec0, 0xff684ec0, 0xff674ec0,
            0xff674ec1, 0xff674ec1, 0xff674ec1, 0xff664ec1, 0xff664ec1, 0xff664ec1, 0xff664ec2,
            0xff654ec2, 0xff654ec2, 0xff654ec2, 0xff654ec2, 0xff654ec3, 0xff644dc3, 0xff644dc3,
            0xff644dc3, 0xff634dc3, 0xff634dc3, 0xff634dc4, 0xff634dc4, 0xff634dc4, 0xff624dc4,
            0xff624dc4, 0xff624dc5, 0xff624dc5, 0xff614dc5, 0xff614dc5, 0xff614cc5, 0xff614cc6,
            0xff604cc6, 0xff604cc6, 0xff604cc6, 0xff604cc6, 0xff5f4cc6, 0xff5f4cc7, 0xff5f4cc7,
            0xff5f4cc7, 0xff5e4cc7, 0xff5e4cc7, 0xff5e4cc8, 0xff5e4cc8, 0xff5d4cc8, 0xff5d4bc8,
            0xff5d4bc8, 0xff5d4bc8, 0xff5c4bc9, 0xff5c4bc9, 0xff5c4bc9, 0xff5c4bc9, 0xff5b4bc9,
            0xff5b4bca, 0xff5b4bca, 0xff5b4bca, 0xff5b4bca, 0xff5a4bca, 0xff5a4aca, 0xff5a4acb,
            0xff5a4acb, 0xff594acb, 0xff594acb, 0xff594acb, 0xff594acc, 0xff584acc, 0xff584acc,
            0xff584acc, 0xff584acc, 0xff574acc, 0xff5749cd, 0xff5749cd, 0xff5749cd, 0xff5649cd,
            0xff5649cd, 0xff5649ce, 0xff5649ce, 0xff5549ce, 0xff5549ce, 0xff5549ce, 0xff5549cf,
            0xff5449cf, 0xff5448cf, 0xff5448cf, 0xff5448cf, 0xff5348cf, 0xff5348d0, 0xff5348d0,
            0xff5348d0, 0xff5248d0, 0xff5248d0, 0xff5248d1, 0xff5247d1, 0xff5147d1, 0xff5147d1,
            0xff5147d1, 0xff5147d2, 0xff5047d2, 0xff5047d2, 0xff5047d2, 0xff5047d2, 0xff4f47d2,
            0xff4f46d3, 0xff4f46d3, 0xff4f46d3, 0xff4e46d3, 0xff4e46d3, 0xff4e46d4, 0xff4e46d4,
            0xff4d46d4, 0xff4d46d4, 0xff4d46d4, 0xff4d45d4, 0xff4c45d5, 0xff4c45d5, 0xff4c45d5,
            0xff4c45d5, 0xff4b45d5, 0xff4b45d5, 0xff4b45d6, 0xff4b45d6, 0xff4a44d6, 0xff4a44d6,
            0xff4a44d6, 0xff4a44d7, 0xff4944d7, 0xff4944d7, 0xff4944d7, 0xff4944d7, 0xff4843d8,
            0xff4843d8, 0xff4843d8, 0xff4843d8, 0xff4743d8, 0xff4743d9, 0xff4743d9, 0xff4743d9,
            0xff4643d9, 0xff4642d9, 0xff4642d9, 0xff4642da, 0xff4542da, 0xff4542da, 0xff4542da,
            0xff4542da, 0xff4442da, 0xff4441db, 0xff4441db, 0xff4341db, 0xff4341db, 0xff4341db,
            0xff4341dc, 0xff4241dc, 0xff4241dc, 0xff4240dc, 0xff4240dc, 0xff4140dc, 0xff4140dd,
            0xff4140dd, 0xff4140dd, 0xff4040dd, 0xff403fdd, 0xff403fde, 0xff403fde, 0xff3f3fde,
            0xff3f3fde, 0xff3f3fde, 0xff3f3fdf, 0xff3e3edf, 0xff3e3edf, 0xff3e3edf, 0xff3d3edf,
            0xff3d3edf, 0xff3d3ee0, 0xff3d3ee0, 0xff3c3de0, 0xff3c3de0, 0xff3c3de0, 0xff3c3de0,
            0xff3b3de1, 0xff3b3de1, 0xff3b3de1, 0xff3b3ce1, 0xff3a3ce1, 0xff3a3ce2, 0xff3a3ce2,
            0xff393ce2, 0xff393ce2, 0xff393be2, 0xff393be3, 0xff383be3, 0xff383be3, 0xff383be3,
            0xff383be3, 0xff373ae3, 0xff373ae4, 0xff373ae4, 0xff363ae4, 0xff363ae4, 0xff363ae4,
            0xff3639e4, 0xff3539e5, 0xff3539e5, 0xff3539e5, 0xff3539e5, 0xff3439e5, 0xff3438e6,
            0xff3438e6, 0xff3338e6, 0xff3338e6, 0xff3338e6, 0xff3337e6, 0xff3237e7, 0xff3237e7,
            0xff3237e7, 0xff3137e7, 0xff3137e7, 0xff3136e8, 0xff3136e8, 0xff3036e8, 0xff3036e8,
            0xff3036e8, 0xff3035e8, 0xff2f35e9, 0xff2f35e9, 0xff2f35e9, 0xff2e35e9, 0xff2e34e9,
            0xff2e34ea, 0xff2e34ea, 0xff2d34ea, 0xff2d34ea, 0xff2d33ea, 0xff2c33eb, 0xff2c33eb,
            0xff2c33eb, 0xff2b32eb, 0xff2b32eb, 0xff2b32eb, 0xff2b32ec, 0xff2a32ec, 0xff2a31ec,
            0xff2a31ec, 0xff2931ec, 0xff2931ec, 0xff2931ed, 0xff2930ed, 0xff2830ed, 0xff2830ed,
            0xff2830ed, 0xff272fee, 0xff272fee, 0xff272fee, 0xff262fee, 0xff262eee, 0xff262eef,
            0xff252eef, 0xff252def, 0xff252def, 0xff252def, 0xff242def, 0xff242cf0, 0xff242cf0,
            0xff232cf0, 0xff232cf0, 0xff232bf0, 0xff222bf1, 0xff222bf1, 0xff222bf1, 0xff212af1,
            0xff212af1, 0xff212af1, 0xff202af1, 0xff2029f2, 0xff2029f2, 0xff1f29f2, 0xff1f28f2,
            0xff1f28f2, 0xff1e28f3, 0xff1e27f3, 0xff1e27f3, 0xff1d27f3, 0xff1d27f3, 0xff1d26f3,
            0xff1c26f4, 0xff1c25f4, 0xff1c25f4, 0xff1b25f4, 0xff1b25f4, 0xff1b24f5, 0xff1a24f5,
            0xff1a24f5, 0xff1923f5, 0xff1923f5, 0xff1922f6, 0xff1822f6, 0xff1822f6, 0xff1821f6,
            0xff1721f6, 0xff1720f6, 0xff1620f7, 0xff1620f7, 0xff161ff7, 0xff151ff7, 0xff151ef7,
            0xff141ef8, 0xff141ef8, 0xff141df8, 0xff131df8, 0xff131cf8, 0xff121cf8, 0xff121bf9,
            0xff111bf9, 0xff111af9, 0xff111af9, 0xff1019f9, 0xff1019fa, 0xff0f18fa, 0xff0f18fa,
            0xff0e17fa, 0xff0e17fa, 0xff0d16fa, 0xff0d15fb, 0xff0c15fb, 0xff0c14fb, 0xff0b14fb,
            0xff0b13fb, 0xff0a12fc, 0xff0a11fc, 0xff0911fc, 0xff0810fc, 0xff080ffc, 0xff070efc,
            0xff070dfd, 0xff060dfd, 0xff060cfd, 0xff050bfd, 0xff0509fd, 0xff0409fe, 0xff0308fe,
            0xff0307fe, 0xff0206fe, 0xff0204fe, 0xff0103fe, 0xff0102fe, 0xff0001ff, 0xff0000ff,
        )

        repeat(1001) {
            val color = lerp(Color.Red, Color.Blue, it / 1000f)
            val colorLong = color.toArgb().toLong() and 0xFFFFFFFFL
            assertEquals(expected[it], colorLong,
                "Expected fraction $it/1000 to have color " +
                    "0x${expected[it].toString(16)}, but was 0x${colorLong.toString(16)}"
            )
        }
    }

    companion object {
        fun Int.toHexString() = "0x${toUInt().toString(16).padStart(8, '0')}"
    }
}
