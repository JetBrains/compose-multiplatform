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

package androidx.compose.ui.graphics

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class GradientTest {

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun testCountTransparentColorsN() {
        assertEquals(0, countTransparentColors(listOf(Color.Red, Color.Green, Color.Blue)))
        assertEquals(1, countTransparentColors(listOf(Color.Red, Color.Transparent, Color.Blue)))
        assertEquals(
            1,
            countTransparentColors(
                listOf(Color.Red, Color.Blue.copy(alpha = 0f), Color.Blue)
            )
        )
        assertEquals(0, countTransparentColors(listOf(Color.Transparent, Color.Green, Color.Blue)))
        assertEquals(0, countTransparentColors(listOf(Color.Red, Color.Green, Color.Transparent)))
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testCountTransparentColorsO() {
        assertEquals(0, countTransparentColors(listOf(Color.Red, Color.Green, Color.Blue)))
        assertEquals(0, countTransparentColors(listOf(Color.Red, Color.Transparent, Color.Blue)))
        assertEquals(
            0,
            countTransparentColors(
                listOf(Color.Red, Color.Blue.copy(alpha = 0f), Color.Blue)
            )
        )
        assertEquals(0, countTransparentColors(listOf(Color.Transparent, Color.Green, Color.Blue)))
        assertEquals(0, countTransparentColors(listOf(Color.Red, Color.Green, Color.Transparent)))
    }

    @Test
    fun testNoTransparentColorNoStopsReturnsNoStops() {
        // Regardless of OS level, all color values that are not Color.Transparent
        // should produce the same stops
        val result = makeTransparentStops(
            null,
            listOf(Color.Red, Color.Green, Color.Blue, Color.Magenta),
            0
        )
        assertNull(result)
    }

    @Test
    fun testNoTransparentColorWithStopsReturnsSameStops() {
        // Regardless of OS level, all color values that are not Color.Transparent
        // should produce the same stops
        val stops = listOf(0f, 0.25f, 0.3f, 1f)
        val result = makeTransparentStops(
            stops,
            listOf(Color.Red, Color.Green, Color.Blue, Color.Magenta),
            0
        )
        assertSameStops(stops, result!!)
    }

    @Test
    fun testTransparentColorsAtEndsAndNoStopsReturnsNoStops() {
        // Regardless of OS level, all color values that are not Color.Transparent
        // should produce the same stops
        val result = makeTransparentStops(
            null,
            listOf(Color.Transparent, Color.Green, Color.Blue, Color.Transparent),
            0
        )
        assertNull(result)
    }

    @Test
    fun testTransparentColorsAtEndsReturnsSameStops() {
        // Regardless of OS level, all color values that are not Color.Transparent
        // should produce the same stops
        val stops = listOf(0f, 0.25f, 0.3f, 1f)
        val result = makeTransparentStops(
            stops,
            listOf(Color.Transparent, Color.Green, Color.Blue, Color.Transparent),
            0
        )
        assertSameStops(stops, result!!)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTransparentColorsInMiddleProducesNoStopsOnO() {
        // On O and later, no stops should be produced.
        val result = makeTransparentStops(
            null,
            listOf(
                Color.Red,
                Color.Transparent,
                Color.Green,
                Color.Transparent,
                Color.Blue,
                Color.Magenta
            ),
            0 // O always counts 0
        )
        assertNull(result)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTransparentColorsInMiddleAddsNoStopsOnO() {
        val stops = listOf(
            0f,
            0.1f,
            0.2f,
            0.5f,
            0.7f,
            1f
        )
        val result = makeTransparentStops(
            stops,
            listOf(
                Color.Red,
                Color.Transparent,
                Color.Green,
                Color.Transparent,
                Color.Blue,
                Color.Magenta
            ),
            0 // O always counts 0
        )
        assertSameStops(stops, result!!)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun testTransparentColorsInMiddleProducesStopsOnN() {
        val result = makeTransparentStops(
            null,
            listOf(
                Color.Red,
                Color.Transparent,
                Color.Green,
                Color.Transparent,
                Color.Blue,
                Color.Magenta
            ),
            2
        )
        assertNotNull(result)
        assertSameStops(listOf(0f, 0.2f, 0.2f, 0.4f, 0.6f, 0.6f, 0.8f, 1f), result!!)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun testTransparentColorsInMiddleAddsStopsOnN() {
        val result = makeTransparentStops(
            listOf(
                0.05f,
                0.1f,
                0.2f,
                0.5f,
                0.7f,
                1f
            ),
            listOf(
                Color.Red,
                Color.Transparent,
                Color.Green,
                Color.Transparent,
                Color.Blue,
                Color.Magenta
            ),
            2
        )
        assertNotNull(result)
        assertSameStops(listOf(0.05f, 0.1f, 0.1f, 0.2f, 0.5f, 0.5f, 0.7f, 1f), result!!)
    }

    @Test
    fun testNoTransparentColorReturnsSameList() {
        // Regardless of OS level, all color values that are not Color.Transparent
        // should be consumed without modification
        val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Magenta)
        val result = makeTransparentColors(colors, 0)
        assertSameColors(colors, result)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTransparencyOnOReturnsSameList() {
        // All Android Versions O and above should apply no modification of input colors
        // that is the filtered color array is expected to have Color.Transparent values
        val colors = listOf(Color.Transparent, Color.Green, Color.Transparent, Color.Magenta)
        val result = makeTransparentColors(colors, 0)
        assertSameColors(colors, result)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun testTransparencyOnNReturnsNewValues() {
        // All Android Versions N and below should modify the color values and add
        // one transparent value for the middle Transparent
        val result = makeTransparentColors(
            listOf(Color.Transparent, Color.Green, Color.Transparent, Color.Magenta),
            1
        )
        assertSameColors(
            listOf(
                Color.Green.copy(alpha = 0f),
                Color.Green,
                Color.Green.copy(alpha = 0f),
                Color.Magenta.copy(alpha = 0f),
                Color.Magenta
            ),
            result
        )
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun testMultipleTrailingTransparentColorsOnN() {
        val result = makeTransparentColors(
            listOf(Color.Red, Color.Green, Color.Transparent, Color.Transparent),
            1
        )
        assertSameColors(
            listOf(
                Color.Red,
                Color.Green,
                Color.Green.copy(alpha = 0.0f),
                Color.Transparent,
                Color.Transparent
            ),
            result
        )
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun testMultipleLeadingTransparentColorsReturnsPreviousOnN() {
        val result = makeTransparentColors(
            listOf(Color.Transparent, Color.Transparent, Color.Blue, Color.Magenta),
            1
        )
        assertSameColors(
            listOf(
                Color.Transparent,
                Color.Transparent,
                Color.Blue.copy(alpha = 0f),
                Color.Blue,
                Color.Magenta
            ),
            result
        )
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun testTransparentAlternatingColors() {
        val result = makeTransparentColors(
            listOf(
                Color.Transparent,
                Color.Red,
                Color.Transparent,
                Color.Blue,
                Color.Transparent
            ),
            1
        )
        assertSameColors(
            listOf(
                Color.Red.copy(alpha = 0f),
                Color.Red,
                Color.Red.copy(alpha = 0f),
                Color.Blue.copy(alpha = 0f),
                Color.Blue,
                Color.Blue.copy(alpha = 0f)
            ),
            result
        )
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun testAllTransparentColorsProduceTransparentOnN() {
        val result = makeTransparentColors(
            listOf(
                Color.Transparent,
                Color.Transparent,
                Color.Transparent
            ),
            1
        )
        assertSameColors(
            listOf(
                Color.Transparent,
                Color.Transparent,
                Color.Transparent,
                Color.Transparent
            ),
            result
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testAllTransparentColorsProduceTransparentOnO() {
        val result = makeTransparentColors(
            listOf(
                Color.Transparent,
                Color.Transparent,
                Color.Transparent
            ),
            0 // Always 0 on O+
        )
        assertSameColors(
            listOf(
                Color.Transparent,
                Color.Transparent,
                Color.Transparent
            ),
            result
        )
    }

    private fun assertSameStops(expected: List<Float>, actual: FloatArray) {
        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { index, value ->
            assertEquals(
                "Stop[$index] expected to be $value, but was ${actual[index]}",
                value,
                actual[index]
            )
        }
    }

    private fun assertSameColors(expected: List<Color>, actual: IntArray) {
        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { index, color ->
            assertEquals(
                "Color[$index] expected to be $color, but was ${Color(actual[index])}",
                color.toArgb(),
                actual[index]
            )
        }
    }
}