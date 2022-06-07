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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertFailsWith

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
}