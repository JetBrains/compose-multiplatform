/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3.windowsizeclass

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WindowSizeClassTest {

    @Test
    fun calculateWidthSizeClass_forNegativeWidth_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            WindowWidthSizeClass.fromWidth((-10).dp)
        }
    }

    @Test
    fun calculateHeightSizeClass_forNegativeWidth_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            WindowHeightSizeClass.fromHeight((-10).dp)
        }
    }

    @Test
    fun calculateWidthSizeClass() {
        assertThat(WindowWidthSizeClass.fromWidth(0.dp)).isEqualTo(WindowWidthSizeClass.Compact)
        assertThat(WindowWidthSizeClass.fromWidth(200.dp)).isEqualTo(WindowWidthSizeClass.Compact)

        assertThat(WindowWidthSizeClass.fromWidth(600.dp)).isEqualTo(WindowWidthSizeClass.Medium)
        assertThat(WindowWidthSizeClass.fromWidth(700.dp)).isEqualTo(WindowWidthSizeClass.Medium)

        assertThat(WindowWidthSizeClass.fromWidth(840.dp)).isEqualTo(WindowWidthSizeClass.Expanded)
        assertThat(WindowWidthSizeClass.fromWidth(1000.dp)).isEqualTo(WindowWidthSizeClass.Expanded)
    }

    @Test
    fun calculateHeightSizeClass() {
        assertThat(WindowHeightSizeClass.fromHeight(0.dp)).isEqualTo(WindowHeightSizeClass.Compact)
        assertThat(WindowHeightSizeClass.fromHeight(200.dp))
            .isEqualTo(WindowHeightSizeClass.Compact)

        assertThat(WindowHeightSizeClass.fromHeight(480.dp)).isEqualTo(WindowHeightSizeClass.Medium)
        assertThat(WindowHeightSizeClass.fromHeight(700.dp))
            .isEqualTo(WindowHeightSizeClass.Medium)

        assertThat(WindowHeightSizeClass.fromHeight(900.dp))
            .isEqualTo(WindowHeightSizeClass.Expanded)
        assertThat(WindowHeightSizeClass.fromHeight(1000.dp))
            .isEqualTo(WindowHeightSizeClass.Expanded)
    }

    @Test
    fun widthSizeClassToString() {
        assertThat(WindowWidthSizeClass.Compact.toString())
            .isEqualTo("WindowWidthSizeClass.Compact")
        assertThat(WindowWidthSizeClass.Medium.toString())
            .isEqualTo("WindowWidthSizeClass.Medium")
        assertThat(WindowWidthSizeClass.Expanded.toString())
            .isEqualTo("WindowWidthSizeClass.Expanded")
    }

    @Test
    fun heightSizeClassToString() {
        assertThat(WindowHeightSizeClass.Compact.toString())
            .isEqualTo("WindowHeightSizeClass.Compact")
        assertThat(WindowHeightSizeClass.Medium.toString())
            .isEqualTo("WindowHeightSizeClass.Medium")
        assertThat(WindowHeightSizeClass.Expanded.toString())
            .isEqualTo("WindowHeightSizeClass.Expanded")
    }

    @Test
    fun widthSizeClassCompareTo() {
        // Less than
        assertThat(WindowWidthSizeClass.Compact < WindowWidthSizeClass.Medium).isTrue()
        assertThat(WindowWidthSizeClass.Compact < WindowWidthSizeClass.Expanded).isTrue()
        assertThat(WindowWidthSizeClass.Medium < WindowWidthSizeClass.Expanded).isTrue()

        assertThat(WindowWidthSizeClass.Compact < WindowWidthSizeClass.Compact).isFalse()
        assertThat(WindowWidthSizeClass.Medium < WindowWidthSizeClass.Medium).isFalse()
        assertThat(WindowWidthSizeClass.Expanded < WindowWidthSizeClass.Expanded).isFalse()

        assertThat(WindowWidthSizeClass.Expanded < WindowWidthSizeClass.Medium).isFalse()
        assertThat(WindowWidthSizeClass.Expanded < WindowWidthSizeClass.Compact).isFalse()
        assertThat(WindowWidthSizeClass.Medium < WindowWidthSizeClass.Compact).isFalse()

        // Less than or equal to
        assertThat(WindowWidthSizeClass.Compact <= WindowWidthSizeClass.Compact).isTrue()
        assertThat(WindowWidthSizeClass.Compact <= WindowWidthSizeClass.Medium).isTrue()
        assertThat(WindowWidthSizeClass.Compact <= WindowWidthSizeClass.Expanded).isTrue()
        assertThat(WindowWidthSizeClass.Medium <= WindowWidthSizeClass.Medium).isTrue()
        assertThat(WindowWidthSizeClass.Medium <= WindowWidthSizeClass.Expanded).isTrue()
        assertThat(WindowWidthSizeClass.Expanded <= WindowWidthSizeClass.Expanded).isTrue()

        assertThat(WindowWidthSizeClass.Expanded <= WindowWidthSizeClass.Medium).isFalse()
        assertThat(WindowWidthSizeClass.Expanded <= WindowWidthSizeClass.Compact).isFalse()
        assertThat(WindowWidthSizeClass.Medium <= WindowWidthSizeClass.Compact).isFalse()

        // Greater than
        assertThat(WindowWidthSizeClass.Expanded > WindowWidthSizeClass.Medium).isTrue()
        assertThat(WindowWidthSizeClass.Expanded > WindowWidthSizeClass.Compact).isTrue()
        assertThat(WindowWidthSizeClass.Medium > WindowWidthSizeClass.Compact).isTrue()

        assertThat(WindowWidthSizeClass.Expanded > WindowWidthSizeClass.Expanded).isFalse()
        assertThat(WindowWidthSizeClass.Medium > WindowWidthSizeClass.Medium).isFalse()
        assertThat(WindowWidthSizeClass.Compact > WindowWidthSizeClass.Compact).isFalse()

        assertThat(WindowWidthSizeClass.Compact > WindowWidthSizeClass.Medium).isFalse()
        assertThat(WindowWidthSizeClass.Compact > WindowWidthSizeClass.Expanded).isFalse()
        assertThat(WindowWidthSizeClass.Medium > WindowWidthSizeClass.Expanded).isFalse()

        // Greater than or equal to
        assertThat(WindowWidthSizeClass.Expanded >= WindowWidthSizeClass.Expanded).isTrue()
        assertThat(WindowWidthSizeClass.Expanded >= WindowWidthSizeClass.Medium).isTrue()
        assertThat(WindowWidthSizeClass.Expanded >= WindowWidthSizeClass.Compact).isTrue()
        assertThat(WindowWidthSizeClass.Medium >= WindowWidthSizeClass.Medium).isTrue()
        assertThat(WindowWidthSizeClass.Medium >= WindowWidthSizeClass.Compact).isTrue()
        assertThat(WindowWidthSizeClass.Compact >= WindowWidthSizeClass.Compact).isTrue()

        assertThat(WindowWidthSizeClass.Compact >= WindowWidthSizeClass.Medium).isFalse()
        assertThat(WindowWidthSizeClass.Compact >= WindowWidthSizeClass.Expanded).isFalse()
        assertThat(WindowWidthSizeClass.Medium >= WindowWidthSizeClass.Expanded).isFalse()
    }

    @Test
    fun heightSizeClassCompareTo() {
        // Less than
        assertThat(WindowHeightSizeClass.Compact < WindowHeightSizeClass.Medium).isTrue()
        assertThat(WindowHeightSizeClass.Compact < WindowHeightSizeClass.Expanded).isTrue()
        assertThat(WindowHeightSizeClass.Medium < WindowHeightSizeClass.Expanded).isTrue()

        assertThat(WindowHeightSizeClass.Compact < WindowHeightSizeClass.Compact).isFalse()
        assertThat(WindowHeightSizeClass.Medium < WindowHeightSizeClass.Medium).isFalse()
        assertThat(WindowHeightSizeClass.Expanded < WindowHeightSizeClass.Expanded).isFalse()

        assertThat(WindowHeightSizeClass.Expanded < WindowHeightSizeClass.Medium).isFalse()
        assertThat(WindowHeightSizeClass.Expanded < WindowHeightSizeClass.Compact).isFalse()
        assertThat(WindowHeightSizeClass.Medium < WindowHeightSizeClass.Compact).isFalse()

        // Less than or equal to
        assertThat(WindowHeightSizeClass.Compact <= WindowHeightSizeClass.Compact).isTrue()
        assertThat(WindowHeightSizeClass.Compact <= WindowHeightSizeClass.Medium).isTrue()
        assertThat(WindowHeightSizeClass.Compact <= WindowHeightSizeClass.Expanded).isTrue()
        assertThat(WindowHeightSizeClass.Medium <= WindowHeightSizeClass.Medium).isTrue()
        assertThat(WindowHeightSizeClass.Medium <= WindowHeightSizeClass.Expanded).isTrue()
        assertThat(WindowHeightSizeClass.Expanded <= WindowHeightSizeClass.Expanded).isTrue()

        assertThat(WindowHeightSizeClass.Expanded <= WindowHeightSizeClass.Medium).isFalse()
        assertThat(WindowHeightSizeClass.Expanded <= WindowHeightSizeClass.Compact).isFalse()
        assertThat(WindowHeightSizeClass.Medium <= WindowHeightSizeClass.Compact).isFalse()

        // Greater than
        assertThat(WindowHeightSizeClass.Expanded > WindowHeightSizeClass.Medium).isTrue()
        assertThat(WindowHeightSizeClass.Expanded > WindowHeightSizeClass.Compact).isTrue()
        assertThat(WindowHeightSizeClass.Medium > WindowHeightSizeClass.Compact).isTrue()

        assertThat(WindowHeightSizeClass.Expanded > WindowHeightSizeClass.Expanded).isFalse()
        assertThat(WindowHeightSizeClass.Medium > WindowHeightSizeClass.Medium).isFalse()
        assertThat(WindowHeightSizeClass.Compact > WindowHeightSizeClass.Compact).isFalse()

        assertThat(WindowHeightSizeClass.Compact > WindowHeightSizeClass.Medium).isFalse()
        assertThat(WindowHeightSizeClass.Compact > WindowHeightSizeClass.Expanded).isFalse()
        assertThat(WindowHeightSizeClass.Medium > WindowHeightSizeClass.Expanded).isFalse()

        // Greater than or equal to
        assertThat(WindowHeightSizeClass.Expanded >= WindowHeightSizeClass.Expanded).isTrue()
        assertThat(WindowHeightSizeClass.Expanded >= WindowHeightSizeClass.Medium).isTrue()
        assertThat(WindowHeightSizeClass.Expanded >= WindowHeightSizeClass.Compact).isTrue()
        assertThat(WindowHeightSizeClass.Medium >= WindowHeightSizeClass.Medium).isTrue()
        assertThat(WindowHeightSizeClass.Medium >= WindowHeightSizeClass.Compact).isTrue()
        assertThat(WindowHeightSizeClass.Compact >= WindowHeightSizeClass.Compact).isTrue()

        assertThat(WindowHeightSizeClass.Compact >= WindowHeightSizeClass.Medium).isFalse()
        assertThat(WindowHeightSizeClass.Compact >= WindowHeightSizeClass.Expanded).isFalse()
        assertThat(WindowHeightSizeClass.Medium >= WindowHeightSizeClass.Expanded).isFalse()
    }
}