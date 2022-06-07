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

package androidx.compose.foundation.layout

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
    fun valueInsets_toString() {
        val insetsValues = InsetsValues(10, 11, 12, 13)
        val insets = ValueInsets(insetsValues, "hello")

        assertThat(insets.toString()).isEqualTo("hello(left=10, top=11, right=12, bottom=13)")
    }

    @Test
    fun valueInsets_toString_afterUpdate() {
        val insetsValues = InsetsValues(10, 11, 12, 13)
        val insets = ValueInsets(insetsValues, "hello")
        insets.value = InsetsValues(20, 21, 22, 23)

        assertThat(insets.toString()).isEqualTo("hello(left=20, top=21, right=22, bottom=23)")
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
    fun fixedIntInsets_toString() {
        val insets = WindowInsets(10, 11, 12, 13)

        assertThat(insets.toString()).isEqualTo("Insets(left=10, top=11, right=12, bottom=13)")
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
    fun fixedDpInsets_toString() {
        val insets = WindowInsets(10.dp, 11.dp, 12.dp, 13.dp)

        assertThat(insets.toString()).isEqualTo(
            "Insets(left=10.0.dp, top=11.0.dp, right=12.0.dp, bottom=13.0.dp)"
        )
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
    fun union_toString() {
        val first = WindowInsets(10, 11, 12, 13)
        val second = WindowInsets(5, 20, 14, 2)
        val union = first.union(second)
        assertThat(union.toString()).isEqualTo(
            "(Insets(left=10, top=11, right=12, bottom=13) ∪ " +
                "Insets(left=5, top=20, right=14, bottom=2))"
        )
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
    fun exclude_toString() {
        val first = WindowInsets(10, 11, 12, 13)
        val second = WindowInsets(5, 20, 14, 2)
        val exclude = first.exclude(second)
        assertThat(exclude.toString()).isEqualTo(
            "(Insets(left=10, top=11, right=12, bottom=13) - " +
                "Insets(left=5, top=20, right=14, bottom=2))"
        )
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
    fun add_toString() {
        val first = WindowInsets(10, 11, 12, 13)
        val second = WindowInsets(5, 20, 14, 2)
        val add = first.add(second)
        assertThat(add.toString()).isEqualTo(
            "(Insets(left=10, top=11, right=12, bottom=13) + " +
                "Insets(left=5, top=20, right=14, bottom=2))"
        )
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
    fun limitInsets_toString() {
        val insets = WindowInsets(10, 11, 12, 13)
            .only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
        assertThat(insets.toString()).isEqualTo(
            "(Insets(left=10, top=11, right=12, bottom=13) only " +
                "WindowInsetsSides(Start+Top+Bottom))"
        )
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

    @Test
    fun insetsValues_toString() {
        assertThat(InsetsValues(1, 2, 3, 4).toString()).isEqualTo(
            "InsetsValues(left=1, top=2, right=3, bottom=4)"
        )
    }

    @Test
    fun paddingValuesWithDensity() {
        val dpInsets = WindowInsets(20, 22, 24, 26)

        val paddingValues = dpInsets.asPaddingValues(Density(2f))
        assertThat(paddingValues.calculateLeftPadding(LayoutDirection.Ltr)).isEqualTo(10.dp)
        assertThat(paddingValues.calculateTopPadding()).isEqualTo(11.dp)
        assertThat(paddingValues.calculateRightPadding(LayoutDirection.Ltr)).isEqualTo(12.dp)
        assertThat(paddingValues.calculateBottomPadding()).isEqualTo(13.dp)
    }
}
