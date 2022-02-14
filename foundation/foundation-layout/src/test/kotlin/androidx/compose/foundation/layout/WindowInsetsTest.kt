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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.InsetsValues
import androidx.compose.foundation.layout.ValueInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WindowInsetsTest {
    private val density = Density(density = 1f)
    private val doubleDensity = Density(density = 2f)

    @Test
    fun valueInsets() {
        val insetsValues = InsetsValues(10, 11, 12, 13)
        val insets = ValueInsets(insetsValues, "hello")

        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(10)
        assertThat(insets.getTop(density)).isEqualTo(11)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(12)
        assertThat(insets.getBottom(density)).isEqualTo(13)

        assertThat(insets.getLeft(doubleDensity, LayoutDirection.Ltr)).isEqualTo(10)
        assertThat(insets.getLeft(density, LayoutDirection.Rtl)).isEqualTo(10)
    }

    @Test
    fun fixedIntInsets() {
        val insets = WindowInsets(10, 11, 12, 13)

        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(10)
        assertThat(insets.getTop(density)).isEqualTo(11)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(12)
        assertThat(insets.getBottom(density)).isEqualTo(13)

        assertThat(insets.getLeft(doubleDensity, LayoutDirection.Ltr)).isEqualTo(10)
        assertThat(insets.getLeft(density, LayoutDirection.Rtl)).isEqualTo(10)
    }

    @Test
    fun fixedDpInsets() {
        val insets = WindowInsets(10.dp, 11.dp, 12.dp, 13.dp)

        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(10)
        assertThat(insets.getTop(density)).isEqualTo(11)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(12)
        assertThat(insets.getBottom(density)).isEqualTo(13)

        assertThat(insets.getLeft(doubleDensity, LayoutDirection.Ltr)).isEqualTo(20)
        assertThat(insets.getLeft(density, LayoutDirection.Rtl)).isEqualTo(10)
    }

    @Test
    fun union() {
        val first = WindowInsets(10, 11, 12, 13)
        val second = WindowInsets(5, 20, 14, 2)
        val union = first.union(second)
        assertThat(union.getLeft(density, LayoutDirection.Ltr)).isEqualTo(10)
        assertThat(union.getTop(density)).isEqualTo(20)
        assertThat(union.getRight(density, LayoutDirection.Ltr)).isEqualTo(14)
        assertThat(union.getBottom(density)).isEqualTo(13)
    }

    @Test
    fun exclude() {
        val first = WindowInsets(10, 11, 12, 13)
        val second = WindowInsets(5, 20, 14, 2)
        val exclude = first.exclude(second)
        assertThat(exclude.getLeft(density, LayoutDirection.Ltr)).isEqualTo(5)
        assertThat(exclude.getTop(density)).isEqualTo(0)
        assertThat(exclude.getRight(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(exclude.getBottom(density)).isEqualTo(11)
    }

    @Test
    fun add() {
        val first = WindowInsets(10, 11, 12, 13)
        val second = WindowInsets(5, 20, 14, 2)
        val add = first.add(second)
        assertThat(add.getLeft(density, LayoutDirection.Ltr)).isEqualTo(15)
        assertThat(add.getTop(density)).isEqualTo(31)
        assertThat(add.getRight(density, LayoutDirection.Ltr)).isEqualTo(26)
        assertThat(add.getBottom(density)).isEqualTo(15)
    }

    @Test
    fun onlyStart() {
        val insets = WindowInsets(10, 11, 12, 13).only(WindowInsetsSides.Start)
        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(10)
        assertThat(insets.getTop(density)).isEqualTo(0)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(insets.getBottom(density)).isEqualTo(0)
        assertThat(insets.getLeft(density, LayoutDirection.Rtl)).isEqualTo(0)
        assertThat(insets.getRight(density, LayoutDirection.Rtl)).isEqualTo(12)
    }

    @Test
    fun onlyTop() {
        val insets = WindowInsets(10, 11, 12, 13).only(WindowInsetsSides.Top)
        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(insets.getTop(density)).isEqualTo(11)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(insets.getBottom(density)).isEqualTo(0)
    }

    @Test
    fun onlyEnd() {
        val insets = WindowInsets(10, 11, 12, 13).only(WindowInsetsSides.End)
        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(insets.getTop(density)).isEqualTo(0)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(12)
        assertThat(insets.getBottom(density)).isEqualTo(0)
        assertThat(insets.getLeft(density, LayoutDirection.Rtl)).isEqualTo(10)
        assertThat(insets.getRight(density, LayoutDirection.Rtl)).isEqualTo(0)
    }

    @Test
    fun onlyBottom() {
        val insets = WindowInsets(10, 11, 12, 13).only(WindowInsetsSides.Bottom)
        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(insets.getTop(density)).isEqualTo(0)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(insets.getBottom(density)).isEqualTo(13)
    }

    @Test
    fun onlyLeft() {
        val insets = WindowInsets(10, 11, 12, 13).only(WindowInsetsSides.Left)
        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(10)
        assertThat(insets.getTop(density)).isEqualTo(0)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(insets.getBottom(density)).isEqualTo(0)
        assertThat(insets.getLeft(density, LayoutDirection.Rtl)).isEqualTo(10)
        assertThat(insets.getRight(density, LayoutDirection.Rtl)).isEqualTo(0)
    }

    @Test
    fun onlyRight() {
        val insets = WindowInsets(10, 11, 12, 13).only(WindowInsetsSides.Right)
        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(insets.getTop(density)).isEqualTo(0)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(12)
        assertThat(insets.getBottom(density)).isEqualTo(0)
        assertThat(insets.getLeft(density, LayoutDirection.Rtl)).isEqualTo(0)
        assertThat(insets.getRight(density, LayoutDirection.Rtl)).isEqualTo(12)
    }

    @Test
    fun onlyHorizontal() {
        val insets = WindowInsets(10, 11, 12, 13).only(WindowInsetsSides.Horizontal)
        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(10)
        assertThat(insets.getTop(density)).isEqualTo(0)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(12)
        assertThat(insets.getBottom(density)).isEqualTo(0)
        assertThat(insets.getLeft(density, LayoutDirection.Rtl)).isEqualTo(10)
        assertThat(insets.getRight(density, LayoutDirection.Rtl)).isEqualTo(12)
    }

    @Test
    fun onlyVertical() {
        val insets = WindowInsets(10, 11, 12, 13).only(WindowInsetsSides.Vertical)
        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(insets.getTop(density)).isEqualTo(11)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertThat(insets.getBottom(density)).isEqualTo(13)
    }

    @Test
    fun plus() {
        val insets = WindowInsets(10, 11, 12, 13)
            .only(WindowInsetsSides.Vertical + WindowInsetsSides.Horizontal)
        assertThat(insets.getLeft(density, LayoutDirection.Ltr)).isEqualTo(10)
        assertThat(insets.getTop(density)).isEqualTo(11)
        assertThat(insets.getRight(density, LayoutDirection.Ltr)).isEqualTo(12)
        assertThat(insets.getBottom(density)).isEqualTo(13)
    }
}
