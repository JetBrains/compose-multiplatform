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
package androidx.compose.ui.text.font

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FontWeightTest {
    @Test
    fun `constructor accept 1000`() {
        assertThat(FontWeight(1000).weight).isEqualTo(1000)
    }

    @Test
    fun `constructor accept 1`() {
        assertThat(FontWeight(1).weight).isEqualTo(1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor does not accept greater than 1000`() {
        FontWeight(1001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor does not accept less than 1`() {
        FontWeight(0)
    }

    @Test
    fun `lerp at start returns start value`() {
        assertThat(
            lerp(
                FontWeight.W200,
                FontWeight.W400,
                0.0f
            )
        ).isEqualTo(FontWeight.W200)
    }

    @Test
    fun `lerp at start returns font weight 1`() {
        val start = FontWeight(1)
        assertThat(lerp(start, FontWeight.W400, 0.0f)).isEqualTo(start)
    }

    @Test
    fun `lerp at end returns end value`() {
        assertThat(
            lerp(
                FontWeight.W200,
                FontWeight.W400,
                1.0f
            )
        ).isEqualTo(FontWeight.W400)
    }

    @Test
    fun `lerp in the mid-time`() {
        assertThat(
            lerp(
                FontWeight.W200,
                FontWeight.W800,
                0.5f
            )
        ).isEqualTo(FontWeight.W500)
    }

    @Test
    fun `lerp in the mid-time with odd distance should be rounded to up`() {
        val start = FontWeight.W200
        val stop = FontWeight.W900
        assertThat(
            lerp(
                start,
                stop,
                0.5f
            )
        ).isEqualTo(FontWeight(((stop.weight + start.weight) * 0.5).toInt()))
    }

    @Test
    fun `values return all weights`() {
        val expectedValues = listOf(
            FontWeight.W100,
            FontWeight.W200,
            FontWeight.W300,
            FontWeight.W400,
            FontWeight.W500,
            FontWeight.W600,
            FontWeight.W700,
            FontWeight.W800,
            FontWeight.W900
        )
        assertThat(FontWeight.values).isEqualTo(expectedValues)
    }

    @Test
    fun `weight returns collect values`() {
        val fontWeights = mapOf(
            FontWeight.W100 to 100,
            FontWeight.W200 to 200,
            FontWeight.W300 to 300,
            FontWeight.W400 to 400,
            FontWeight.W500 to 500,
            FontWeight.W600 to 600,
            FontWeight.W700 to 700,
            FontWeight.W800 to 800,
            FontWeight.W900 to 900
        )

        // TODO(b/130795950): IR compiler bug was here
        for (weightPair in fontWeights) {
            val (fontWeight, expectedWeight) = weightPair
            assertThat(fontWeight.weight).isEqualTo(expectedWeight)
        }
    }

    @Test
    fun compareTo() {
        assertThat(FontWeight.W400.compareTo(FontWeight.W400)).isEqualTo(0)
        assertThat(FontWeight.W400.compareTo(FontWeight.W300)).isEqualTo(1)
        assertThat(FontWeight.W400.compareTo(FontWeight.W500)).isEqualTo(-1)
    }
}