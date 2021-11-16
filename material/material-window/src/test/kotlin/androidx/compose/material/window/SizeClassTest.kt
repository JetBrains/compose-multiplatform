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

package androidx.compose.material.window

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertFailsWith

@RunWith(JUnit4::class)
class SizeClassTest {

    @Test
    fun calculateWidthSizeClass_forNegativeWidth_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            WidthSizeClass.fromWidth((-10).dp)
        }
    }

    @Test
    fun calculateHeightSizeClass_forNegativeWidth_throws() {
        assertFailsWith(IllegalArgumentException::class) {
            HeightSizeClass.fromHeight((-10).dp)
        }
    }

    @Test
    fun calculateWidthSizeClass() {
        assertThat(WidthSizeClass.fromWidth(0.dp)).isEqualTo(WidthSizeClass.Compact)
        assertThat(WidthSizeClass.fromWidth(200.dp)).isEqualTo(WidthSizeClass.Compact)

        assertThat(WidthSizeClass.fromWidth(600.dp)).isEqualTo(WidthSizeClass.Medium)
        assertThat(WidthSizeClass.fromWidth(700.dp)).isEqualTo(WidthSizeClass.Medium)

        assertThat(WidthSizeClass.fromWidth(840.dp)).isEqualTo(WidthSizeClass.Expanded)
        assertThat(WidthSizeClass.fromWidth(1000.dp)).isEqualTo(WidthSizeClass.Expanded)
    }

    @Test
    fun calculateHeightSizeClass() {
        assertThat(HeightSizeClass.fromHeight(0.dp)).isEqualTo(HeightSizeClass.Compact)
        assertThat(HeightSizeClass.fromHeight(200.dp)).isEqualTo(HeightSizeClass.Compact)

        assertThat(HeightSizeClass.fromHeight(480.dp)).isEqualTo(HeightSizeClass.Medium)
        assertThat(HeightSizeClass.fromHeight(700.dp)).isEqualTo(HeightSizeClass.Medium)

        assertThat(HeightSizeClass.fromHeight(900.dp)).isEqualTo(HeightSizeClass.Expanded)
        assertThat(HeightSizeClass.fromHeight(1000.dp)).isEqualTo(HeightSizeClass.Expanded)
    }
}