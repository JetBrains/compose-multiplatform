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

package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.colorspace.Adaptation
import androidx.compose.ui.graphics.colorspace.ColorModel
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.ColorSpaces.ColorSpacesArray
import androidx.compose.ui.graphics.colorspace.ColorSpaces.SrgbTransferParameters
import androidx.compose.ui.graphics.colorspace.Connector
import androidx.compose.ui.graphics.colorspace.Illuminant
import androidx.compose.ui.graphics.colorspace.RenderIntent
import androidx.compose.ui.graphics.colorspace.Rgb
import androidx.compose.ui.graphics.colorspace.TransferParameters
import androidx.compose.ui.graphics.colorspace.WhitePoint
import androidx.compose.ui.graphics.colorspace.adapt
import androidx.compose.ui.graphics.colorspace.connect
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class ColorSpaceTest {

    @Test
    fun testNamedColorSpaces() {
        ColorSpaces.ColorSpacesArray.forEachIndexed { index, colorSpace ->
            assertNotNull(colorSpace.name)
            assertNotNull(colorSpace)
            assertEquals(index, colorSpace.id)
            assertTrue(colorSpace.componentCount >= 1)
            assertTrue(colorSpace.componentCount <= 4)
        }
    }

    @Test
    fun testEmptyName() {
        assertFailsWith<IllegalArgumentException> {
            Rgb(
                "",
                FloatArray(6),
                WhitePoint(0f, 0f),
                sIdentity,
                sIdentity,
                0.0f,
                1.0f
            )
        }
    }

    @Test
    fun testName() {
        val cs = Rgb(
            "Test", FloatArray(6), WhitePoint(0f, 0f),
            sIdentity, sIdentity, 0.0f, 1.0f
        )
        assertEquals("Test", cs.name)
    }

    @Test
    fun testPrimariesLength() {
        assertFailsWith<IllegalArgumentException> {
            Rgb(
                "Test",
                FloatArray(7),
                WhitePoint(0f, 0f),
                sIdentity,
                sIdentity,
                0.0f,
                1.0f
            )
        }
    }

    @Test
    fun testOETF() {
        val op: (Double) -> Double = { x -> sqrt(x) }
        val cs = Rgb(
            "Test", FloatArray(6), WhitePoint(0f, 0f),
            op, sIdentity, 0.0f, 1.0f
        )
        assertEquals(0.5, cs.oetf(0.25), 1e-5)
    }

    @Test
    fun testEOTF() {
        val op: (Double) -> Double = { x -> x * x }
        val cs = Rgb(
            "Test", FloatArray(6), WhitePoint(0f, 0f),
            sIdentity, op, 0.0f, 1.0f
        )
        assertEquals(0.0625, cs.eotf(0.25), 1e-5)
    }

    @Test
    fun testInvalidRange() {
        assertFailsWith<IllegalArgumentException> {
            Rgb(
                "Test",
                FloatArray(6),
                WhitePoint(0f, 0f),
                sIdentity,
                sIdentity,
                2.0f,
                1.0f
            )
        }
    }

    @Test
    fun testRanges() {
        var cs: ColorSpace = ColorSpaces.Srgb

        var m1 = cs.getMinValue(0)
        var m2 = cs.getMinValue(1)
        var m3 = cs.getMinValue(2)

        assertEquals(0.0f, m1, 1e-9f)
        assertEquals(0.0f, m2, 1e-9f)
        assertEquals(0.0f, m3, 1e-9f)

        m1 = cs.getMaxValue(0)
        m2 = cs.getMaxValue(1)
        m3 = cs.getMaxValue(2)

        assertEquals(1.0f, m1, 1e-9f)
        assertEquals(1.0f, m2, 1e-9f)
        assertEquals(1.0f, m3, 1e-9f)

        cs = ColorSpaces.CieLab

        m1 = cs.getMinValue(0)
        m2 = cs.getMinValue(1)
        m3 = cs.getMinValue(2)

        assertEquals(0.0f, m1, 1e-9f)
        assertEquals(-128.0f, m2, 1e-9f)
        assertEquals(-128.0f, m3, 1e-9f)

        m1 = cs.getMaxValue(0)
        m2 = cs.getMaxValue(1)
        m3 = cs.getMaxValue(2)

        assertEquals(100.0f, m1, 1e-9f)
        assertEquals(128.0f, m2, 1e-9f)
        assertEquals(128.0f, m3, 1e-9f)

        cs = ColorSpaces.CieXyz

        m1 = cs.getMinValue(0)
        m2 = cs.getMinValue(1)
        m3 = cs.getMinValue(2)

        assertEquals(-2.0f, m1, 1e-9f)
        assertEquals(-2.0f, m2, 1e-9f)
        assertEquals(-2.0f, m3, 1e-9f)

        m1 = cs.getMaxValue(0)
        m2 = cs.getMaxValue(1)
        m3 = cs.getMaxValue(2)

        assertEquals(2.0f, m1, 1e-9f)
        assertEquals(2.0f, m2, 1e-9f)
        assertEquals(2.0f, m3, 1e-9f)
    }

    @Test
    fun testMat3x3() {
        val cs = Rgb("Test", SRGB_TO_XYZ, sIdentity, sIdentity)

        val rgbToXYZ = cs.getTransform()
        for (i in 0..8) {
            assertEquals(SRGB_TO_XYZ[i], rgbToXYZ[i], 1e-5f)
        }
    }

    @Test
    fun testMat3x3Inverse() {
        val cs = Rgb("Test", SRGB_TO_XYZ, sIdentity, sIdentity)

        val xyzToRGB = cs.getInverseTransform()
        for (i in 0..8) {
            assertEquals(XYZ_TO_SRGB[i], xyzToRGB[i], 1e-5f)
        }
    }

    @Test
    fun testMat3x3Primaries() {
        val cs = Rgb("Test", SRGB_TO_XYZ, sIdentity, sIdentity)

        val primaries = cs.getPrimaries()

        assertNotNull(primaries)
        assertEquals(6, primaries.size.toLong())

        assertEquals(SRGB_PRIMARIES_xyY[0], primaries[0], 1e-5f)
        assertEquals(SRGB_PRIMARIES_xyY[1], primaries[1], 1e-5f)
        assertEquals(SRGB_PRIMARIES_xyY[2], primaries[2], 1e-5f)
        assertEquals(SRGB_PRIMARIES_xyY[3], primaries[3], 1e-5f)
        assertEquals(SRGB_PRIMARIES_xyY[4], primaries[4], 1e-5f)
        assertEquals(SRGB_PRIMARIES_xyY[5], primaries[5], 1e-5f)
    }

    @Test
    fun testMat3x3WhitePoint() {
        val cs = Rgb("Test", SRGB_TO_XYZ, sIdentity, sIdentity)

        val whitePoint = cs.whitePoint

        assertNotNull(whitePoint)

        assertEquals(SRGB_WHITE_POINT_xyY.x, whitePoint.x, 1e-5f)
        assertEquals(SRGB_WHITE_POINT_xyY.y, whitePoint.y, 1e-5f)
    }

    @Test
    fun testXYZFromPrimaries_xyY() {
        val cs = Rgb(
            "Test", SRGB_PRIMARIES_xyY, SRGB_WHITE_POINT_xyY,
            sIdentity, sIdentity, 0.0f, 1.0f
        )

        val rgbToXYZ = cs.getTransform()
        for (i in 0..8) {
            assertEquals(SRGB_TO_XYZ[i], rgbToXYZ[i], 1e-5f)
        }

        val xyzToRGB = cs.getInverseTransform()
        for (i in 0..8) {
            assertEquals(XYZ_TO_SRGB[i], xyzToRGB[i], 1e-5f)
        }
    }

    @Test
    fun testXYZFromPrimaries_XYZ() {
        val cs = Rgb(
            "Test", SRGB_PRIMARIES_XYZ, SRGB_WHITE_POINT_XYZ,
            sIdentity, sIdentity, 0.0f, 1.0f
        )

        val primaries = cs.getPrimaries()

        assertNotNull(primaries)
        assertEquals(6, primaries.size.toLong())

        // SRGB_PRIMARIES_xyY only has 1e-3 of precision, match it
        assertEquals(SRGB_PRIMARIES_xyY[0], primaries[0], 1e-3f)
        assertEquals(SRGB_PRIMARIES_xyY[1], primaries[1], 1e-3f)
        assertEquals(SRGB_PRIMARIES_xyY[2], primaries[2], 1e-3f)
        assertEquals(SRGB_PRIMARIES_xyY[3], primaries[3], 1e-3f)
        assertEquals(SRGB_PRIMARIES_xyY[4], primaries[4], 1e-3f)
        assertEquals(SRGB_PRIMARIES_xyY[5], primaries[5], 1e-3f)

        val whitePoint = cs.whitePoint

        assertNotNull(whitePoint)

        // SRGB_WHITE_POINT_xyY only has 1e-3 of precision, match it
        assertEquals(SRGB_WHITE_POINT_xyY.x, whitePoint.x, 1e-3f)
        assertEquals(SRGB_WHITE_POINT_xyY.y, whitePoint.y, 1e-3f)

        val rgbToXYZ = cs.getTransform()
        for (i in 0..8) {
            assertEquals(SRGB_TO_XYZ[i], rgbToXYZ[i], 1e-5f)
        }

        val xyzToRGB = cs.getInverseTransform()
        for (i in 0..8) {
            assertEquals(XYZ_TO_SRGB[i], xyzToRGB[i], 1e-5f)
        }
    }

    @Test
    fun testGetComponentCount() {
        assertEquals(3, ColorSpaces.Srgb.componentCount.toLong())
        assertEquals(3, ColorSpaces.LinearSrgb.componentCount.toLong())
        assertEquals(3, ColorSpaces.ExtendedSrgb.componentCount.toLong())
        assertEquals(
            3,
            ColorSpaces.LinearExtendedSrgb.componentCount.toLong()
        )
        assertEquals(3, ColorSpaces.DisplayP3.componentCount.toLong())
        assertEquals(3, ColorSpaces.CieLab.componentCount.toLong())
        assertEquals(3, ColorSpaces.CieXyz.componentCount.toLong())
    }

    @Test
    fun testIsSRGB() {
        for (colorSpace in ColorSpacesArray) {
            if (colorSpace === ColorSpaces.Srgb) {
                assertTrue(colorSpace.isSrgb)
            } else {
                assertFalse(
                    colorSpace.isSrgb,
                    "Incorrectly treating $colorSpace as SRGB!"
                )
            }
        }

        val cs = Rgb(
            "Almost sRGB", SRGB_TO_XYZ,
            { x -> x.pow(1.0 / 2.2) }, { x -> x.pow(2.2) }
        )
        assertFalse(cs.isSrgb)
    }

    @Test
    fun testIsWideGamut() {
        assertFalse(ColorSpaces.Srgb.isWideGamut)
        assertFalse(ColorSpaces.Bt709.isWideGamut)
        assertTrue(ColorSpaces.ExtendedSrgb.isWideGamut)
        assertTrue(ColorSpaces.DciP3.isWideGamut)
        assertTrue(ColorSpaces.Bt2020.isWideGamut)
        assertTrue(ColorSpaces.Aces.isWideGamut)
        assertTrue(ColorSpaces.CieLab.isWideGamut)
        assertTrue(ColorSpaces.CieXyz.isWideGamut)
    }

    @Test
    fun testWhitePoint() {
        val cs = ColorSpaces.Srgb

        val whitePoint = cs.whitePoint
        assertEquals(Illuminant.D65, whitePoint)
    }

    @Test
    fun testPrimaries() {
        val cs = ColorSpaces.Srgb

        val primaries = cs.getPrimaries()

        assertNotNull(primaries)
        assertEquals(6, primaries.size.toLong())

        // Make sure a copy is returned
        primaries.fill(Float.NaN)
        assertArrayNotEquals(primaries, cs.getPrimaries(), 1e-5f)
        assertSame(primaries, cs.getPrimaries(primaries))
        assertArrayEquals(primaries, cs.getPrimaries(), 1e-5f)
    }

    @Test
    fun testRGBtoXYZMatrix() {
        val cs = ColorSpaces.Srgb

        val rgbToXYZ = cs.getTransform()

        assertNotNull(rgbToXYZ)
        assertEquals(9, rgbToXYZ.size.toLong())

        // Make sure a copy is returned
        rgbToXYZ.fill(Float.NaN)
        assertArrayNotEquals(rgbToXYZ, cs.getTransform(), 1e-5f)
        assertSame(rgbToXYZ, cs.getTransform(rgbToXYZ))
        assertArrayEquals(rgbToXYZ, cs.getTransform(), 1e-5f)
    }

    @Test
    fun testXYZtoRGBMatrix() {
        val cs = ColorSpaces.Srgb

        val xyzToRGB = cs.getInverseTransform()

        assertNotNull(xyzToRGB)
        assertEquals(9, xyzToRGB.size.toLong())

        // Make sure a copy is returned
        xyzToRGB.fill(Float.NaN)
        assertArrayNotEquals(xyzToRGB, cs.getInverseTransform(), 1e-5f)
        assertSame(xyzToRGB, cs.getInverseTransform(xyzToRGB))
        assertArrayEquals(xyzToRGB, cs.getInverseTransform(), 1e-5f)
    }

    @Test
    fun testRGBtoXYZ() {
        val cs = ColorSpaces.Srgb

        val source = floatArrayOf(0.75f, 0.5f, 0.25f)
        val expected = floatArrayOf(0.3012f, 0.2679f, 0.0840f)

        val r1 = cs.toXyz(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayNotEquals(source, r1, 1e-5f)
        assertArrayEquals(expected, r1, 1e-3f)

        val r3 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r3, cs.toXyz(r3))
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(r1, r3, 1e-5f)
    }

    @Test
    fun testXYZtoRGB() {
        val cs = ColorSpaces.Srgb

        val source = floatArrayOf(0.3012f, 0.2679f, 0.0840f)
        val expected = floatArrayOf(0.75f, 0.5f, 0.25f)

        val r1 = cs.fromXyz(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayNotEquals(source, r1, 1e-5f)
        assertArrayEquals(expected, r1, 1e-3f)

        val r3 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r3, cs.fromXyz(r3))
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(r1, r3, 1e-5f)
    }

    @Test
    fun testConnect() {
        var connector: Connector = ColorSpaces.Srgb.connect(ColorSpaces.DciP3)

        assertSame(ColorSpaces.Srgb, connector.source)
        assertSame(ColorSpaces.DciP3, connector.destination)
        assertEquals(RenderIntent.Perceptual, connector.renderIntent)

        connector = ColorSpaces.Srgb.connect(ColorSpaces.Srgb)

        assertSame(connector.destination, connector.source)
        assertEquals(RenderIntent.Relative, connector.renderIntent)

        connector = ColorSpaces.DciP3.connect()
        assertSame(ColorSpaces.Srgb, connector.destination)

        connector = ColorSpaces.Srgb.connect()
        assertSame(connector.source, connector.destination)
    }

    @Test
    fun testConnector() {
        // Connect color spaces with same white points
        var connector: Connector = ColorSpaces.Srgb.connect(ColorSpaces.AdobeRgb)

        var source = floatArrayOf(1.0f, 0.5f, 0.0f)
        var expected = floatArrayOf(0.8912f, 0.4962f, 0.1164f)

        var r1 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayNotEquals(source, r1, 1e-5f)
        assertArrayEquals(expected, r1, 1e-3f)

        var r3 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r3, connector.transform(r3))
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(r1, r3, 1e-5f)

        connector = ColorSpaces.AdobeRgb.connect(ColorSpaces.Srgb)

        val tmp = source
        source = expected
        expected = tmp

        r1 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayNotEquals(source, r1, 1e-5f)
        assertArrayEquals(expected, r1, 1e-3f)

        r3 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r3, connector.transform(r3))
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(r1, r3, 1e-5f)
    }

    @Test
    fun testAdaptedConnector() {
        // Connect color spaces with different white points
        val connector = ColorSpaces.Srgb.connect(ColorSpaces.ProPhotoRgb)

        val source = floatArrayOf(1.0f, 0.0f, 0.0f)
        val expected = floatArrayOf(0.70226f, 0.2757f, 0.1036f)

        val r = connector.transform(source[0], source[1], source[2])
        assertNotNull(r)
        assertEquals(3, r.size.toLong())
        assertArrayNotEquals(source, r, 1e-5f)
        assertArrayEquals(expected, r, 1e-4f)
    }

    @Test
    fun testAdaptedConnectorWithRenderIntent() {
        // Connect a wider color space to a narrow color space
        var connector: Connector = ColorSpaces.DciP3.connect(
            ColorSpaces.Srgb,
            RenderIntent.Relative
        )

        val source = floatArrayOf(0.9f, 0.9f, 0.9f)

        val relative = connector.transform(source[0], source[1], source[2])
        assertNotNull(relative)
        assertEquals(3, relative.size.toLong())
        assertArrayNotEquals(source, relative, 1e-5f)
        assertArrayEquals(floatArrayOf(0.8862f, 0.8862f, 0.8862f), relative, 1e-4f)

        connector = ColorSpaces.DciP3.connect(
            ColorSpaces.Srgb,
            RenderIntent.Absolute
        )

        val absolute = connector.transform(source[0], source[1], source[2])
        assertNotNull(absolute)
        assertEquals(3, absolute.size.toLong())
        assertArrayNotEquals(source, absolute, 1e-5f)
        assertArrayNotEquals(relative, absolute, 1e-5f)
        assertArrayEquals(floatArrayOf(0.8475f, 0.9217f, 0.8203f), absolute, 1e-4f)
    }

    @Test
    fun testIdentityConnector() {
        val connector = ColorSpaces.Srgb.connect(ColorSpaces.Srgb)

        assertSame(connector.source, connector.destination)
        assertEquals(RenderIntent.Relative, connector.renderIntent)

        val source = floatArrayOf(0.11112f, 0.22227f, 0.444448f)

        val r = connector.transform(source[0], source[1], source[2])
        assertNotNull(r)
        assertEquals(3, r.size.toLong())
        assertArrayEquals(source, r, 1e-5f)
    }

    @Test
    fun testConnectorTransformIdentity() {
        val connector = ColorSpaces.DciP3.connect(ColorSpaces.DciP3)

        val source = floatArrayOf(1.0f, 0.0f, 0.0f)
        val expected = floatArrayOf(1.0f, 0.0f, 0.0f)

        val r1 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(expected, r1, 1e-3f)

        val r3 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r3, connector.transform(r3))
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(r1, r3, 1e-5f)
    }

    @Test
    fun testAdaptation() {
        var adapted = ColorSpaces.Srgb.adapt(
            Illuminant.D50
        )

        val sRGBD50 = floatArrayOf(
            0.43602175f,
            0.22247513f,
            0.01392813f,
            0.38510883f,
            0.71690667f,
            0.09710153f,
            0.14308129f,
            0.06061824f,
            0.71415880f
        )

        assertArrayEquals(sRGBD50, (adapted as Rgb).getTransform(), 1e-7f)

        adapted = ColorSpaces.Srgb.adapt(
            Illuminant.D50,
            Adaptation.Bradford
        )
        assertArrayEquals(sRGBD50, (adapted as Rgb).getTransform(), 1e-7f)
    }

    @Test
    fun testImplicitSRGBConnector() {
        val connector1 = ColorSpaces.DciP3.connect()

        assertSame(ColorSpaces.Srgb, connector1.destination)

        val connector2 = ColorSpaces.DciP3.connect(
            ColorSpaces.Srgb
        )

        val source = floatArrayOf(0.6f, 0.9f, 0.7f)
        assertArrayEquals(
            connector1.transform(source[0], source[1], source[2]),
            connector2.transform(source[0], source[1], source[2]), 1e-7f
        )
    }

    @Test
    fun testLab() {
        var connector: Connector = ColorSpaces.CieLab.connect()

        var source = floatArrayOf(100.0f, 0.0f, 0.0f)
        var expected = floatArrayOf(1.0f, 1.0f, 1.0f)

        var r1 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(expected, r1, 1e-3f)

        source = floatArrayOf(100.0f, 0.0f, 54.0f)
        expected = floatArrayOf(1.0f, 0.9925f, 0.5762f)

        var r2 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r2)
        assertEquals(3, r2.size.toLong())
        assertArrayEquals(expected, r2, 1e-3f)

        connector = ColorSpaces.CieLab.connect(intent = RenderIntent.Absolute)

        source = floatArrayOf(100.0f, 0.0f, 0.0f)
        expected = floatArrayOf(1.0f, 0.9910f, 0.8651f)

        r1 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(expected, r1, 1e-3f)

        source = floatArrayOf(100.0f, 0.0f, 54.0f)
        expected = floatArrayOf(1.0f, 0.9853f, 0.4652f)

        r2 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r2)
        assertEquals(3, r2.size.toLong())
        assertArrayEquals(expected, r2, 1e-3f)
    }

    @Test
    fun testOkLab() {
        val connector = ColorSpaces.Oklab.connect()

        // Red+green
        val source1 = floatArrayOf(1.000f, -0.065f, 0.241f)
        val expected1 = floatArrayOf(1.000f, 1.000f, 0.000f)

        val r1 = connector.transform(source1[0], source1[1], source1[2])
        assertArrayEquals(expected1, r1, 1e-3f)

        // Green+blue
        val source2 = floatArrayOf(1.000f, -0.125f, -0.217f)
        val expected2 = floatArrayOf(0.000f, 1.000f, 1.000f)

        val r2 = connector.transform(source2[0], source2[1], source2[2])
        assertArrayEquals(expected2, r2, 1e-3f)

        // Red+blue
        val source3 = floatArrayOf(0.500f, 0.250f, 0.000f)
        val expected3 = floatArrayOf(0.768f, 0.000f, 0.366f)

        val r3 = connector.transform(source3[0], source3[1], source3[2])
        assertArrayEquals(expected3, r3, 1e-3f)

        // White
        val source4 = floatArrayOf(1.000f, 0.000f, 0.000f)
        val expected4 = floatArrayOf(1.000f, 1.000f, 1.000f)

        val r4 = connector.transform(source4[0], source4[1], source4[2])
        assertArrayEquals(expected4, r4, 1e-3f)

        // Black
        val source5 = floatArrayOf(0.000f, 0.000f, 0.000f)
        val expected5 = floatArrayOf(0.000f, 0.000f, 0.000f)

        val r5 = connector.transform(source5[0], source5[1], source5[2])
        assertArrayEquals(expected5, r5, 1e-3f)
    }

    @Test
    fun testXYZ() {
        val xyz = ColorSpaces.CieXyz

        val source = floatArrayOf(0.32f, 0.43f, 0.54f)

        val r1 = xyz.toXyz(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(source, r1, 1e-7f)

        val r2 = xyz.fromXyz(source[0], source[1], source[2])
        assertNotNull(r2)
        assertEquals(3, r2.size.toLong())
        assertArrayEquals(source, r2, 1e-7f)

        val connector = ColorSpaces.CieXyz.connect()

        val expected = floatArrayOf(0.2280f, 0.7541f, 0.8453f)

        val r3 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r3)
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(expected, r3, 1e-3f)
    }

    @Test
    fun testIDs() {
        // These cannot change
        assertEquals(0, ColorSpaces.Srgb.id.toLong())
        assertEquals(-1, ColorSpace.MinId.toLong())
        assertEquals(63, ColorSpace.MaxId.toLong())
    }

    @Test
    fun testFromLinear() {
        val colorSpace = ColorSpaces.Srgb

        val source = floatArrayOf(0.0f, 0.5f, 1.0f)
        val expected = floatArrayOf(0.0f, 0.7354f, 1.0f)

        val r1 = colorSpace.fromLinear(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(expected, r1, 1e-3f)

        val r2 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r2, colorSpace.fromLinear(r2))
        assertEquals(3, r2.size.toLong())
        assertArrayEquals(r1, r2, 1e-5f)
    }

    @Test
    fun testToLinear() {
        val colorSpace = ColorSpaces.Srgb

        val source = floatArrayOf(0.0f, 0.5f, 1.0f)
        val expected = floatArrayOf(0.0f, 0.2140f, 1.0f)

        val r1 = colorSpace.toLinear(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(expected, r1, 1e-3f)

        val r2 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r2, colorSpace.toLinear(r2))
        assertEquals(3, r2.size.toLong())
        assertArrayEquals(r1, r2, 1e-5f)
    }

    @Test
    fun testTransferParameters() {
        var colorSpace = ColorSpaces.Srgb
        assertNotNull(colorSpace.transferParameters)

        colorSpace = ColorSpaces.ExtendedSrgb
        assertSame(SrgbTransferParameters, colorSpace.transferParameters)
    }

    @Test
    fun testIdempotentTransferFunctions() {
        ColorSpacesArray
            .filter { cs -> cs.model == ColorModel.Rgb }
            .map { cs -> cs as Rgb }
            .forEach { cs ->
                val source = floatArrayOf(0.0f, 0.5f, 1.0f)
                val r = cs.fromLinear(cs.toLinear(source[0], source[1], source[2]))
                assertArrayEquals(source, r, 1e-3f)
            }
    }

    @Test
    fun testMatch() {
        for (cs in ColorSpacesArray) {
            if (cs.model == ColorModel.Rgb) {
                var rgb = cs as Rgb
                // match() cannot match extended sRGB
                if (rgb !== ColorSpaces.ExtendedSrgb && rgb !== ColorSpaces.LinearExtendedSrgb) {
                    // match() uses CIE XYZ D50
                    rgb = rgb.adapt(Illuminant.D50) as Rgb
                    assertSame(
                        cs,
                        ColorSpaces.match(rgb.getTransform(), rgb.transferParameters!!)
                    )
                }
            }
        }

        assertSame(
            ColorSpaces.Srgb,
            ColorSpaces.match(
                SRGB_TO_XYZ_D50,
                TransferParameters(2.4, 1 / 1.055, 0.055 / 1.055, 1 / 12.92, 0.04045)
            )
        )
    }

    companion object {
        // Column-major RGB->XYZ transform matrix for the sRGB color space
        private val SRGB_TO_XYZ = floatArrayOf(
            0.412391f,
            0.212639f,
            0.019331f,
            0.357584f,
            0.715169f,
            0.119195f,
            0.180481f,
            0.072192f,
            0.950532f
        )
        // Column-major XYZ->RGB transform matrix for the sRGB color space
        private val XYZ_TO_SRGB = floatArrayOf(
            3.240970f,
            -0.969244f,
            0.055630f,
            -1.537383f,
            1.875968f,
            -0.203977f,
            -0.498611f,
            0.041555f,
            1.056971f
        )

        // Column-major RGB->XYZ transform matrix for the sRGB color space and a D50 white point
        private val SRGB_TO_XYZ_D50 = floatArrayOf(
            0.4360747f,
            0.2225045f,
            0.0139322f,
            0.3850649f,
            0.7168786f,
            0.0971045f,
            0.1430804f,
            0.0606169f,
            0.7141733f
        )

        private val SRGB_PRIMARIES_xyY =
            floatArrayOf(0.640f, 0.330f, 0.300f, 0.600f, 0.150f, 0.060f)
        private val SRGB_WHITE_POINT_xyY = WhitePoint(0.3127f, 0.3290f)

        private val SRGB_PRIMARIES_XYZ = floatArrayOf(
            1.939394f,
            1.000000f,
            0.090909f,
            0.500000f,
            1.000000f,
            0.166667f,
            2.500000f,
            1.000000f,
            13.166667f
        )
        private val SRGB_WHITE_POINT_XYZ = WhitePoint(0.950456f, 1.000f, 1.089058f)

        private val sIdentity: (Double) -> Double = { x -> x }

        private fun assertArrayNotEquals(a: FloatArray, b: FloatArray, eps: Float) {
            for (i in a.indices) {
                if (a[i].compareTo(b[i]) == 0 || abs(a[i] - b[i]) < eps) {
                    fail("Expected " + a[i] + ", received " + b[i])
                }
            }
        }

        private fun assertArrayEquals(a: FloatArray, b: FloatArray, eps: Float) {
            for (i in a.indices) {
                if (a[i].compareTo(b[i]) != 0 && abs(a[i] - b[i]) > eps) {
                    fail("Expected " + a[i] + ", received " + b[i])
                }
            }
        }
    }
}
