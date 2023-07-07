/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CornerBasedShapeTest {

    @Test
    fun createOutlineCalledWithCorrectParams() {
        val density = Density(2f, 1f)
        val passedSize = Size(100.0f, 50.0f)
        var assertionExecuted = false
        val assertSizes = { size: Size,
            topStart: Float,
            topEnd: Float,
            bottomEnd: Float,
            bottomStart: Float,
            ld: LayoutDirection ->
            assertThat(size).isEqualTo(passedSize)
            assertThat(topStart).isEqualTo(4.0f)
            assertThat(topEnd).isEqualTo(3.0f)
            assertThat(bottomEnd).isEqualTo(6.0f)
            assertThat(bottomStart).isEqualTo(25.0f)
            assertThat(ld).isEqualTo(LayoutDirection.Ltr)
            assertionExecuted = true
        }
        val impl = Impl(
            topStart = CornerSize(4.0f),
            topEnd = CornerSize(3.0f),
            bottomEnd = CornerSize(3.dp),
            bottomStart = CornerSize(50),
            onOutlineRequested = assertSizes
        )

        assertThat(impl.createOutline(passedSize, LayoutDirection.Ltr, density))
            .isEqualTo(Outline.Rectangle(passedSize.toRect()))

        assertThat(assertionExecuted).isTrue()
    }

    @Test
    fun topCornersSizesAreNotLargerThenMinDimension() {
        val density = Density(2f, 1f)
        val sizeWithLargerWidth = Size(6.0f, 4.0f)
        val sizeWithLargerHeight = Size(4.0f, 6.0f)

        val sizesList = mutableListOf<Size>()
        val assertSizes = { size: Size,
            topStart: Float,
            topEnd: Float,
            bottomEnd: Float,
            bottomStart: Float,
            ld: LayoutDirection ->
            sizesList.add(size)
            assertThat(topStart).isEqualTo(4.0f)
            assertThat(topEnd).isEqualTo(4.0f)
            assertThat(bottomEnd).isEqualTo(0.0f)
            assertThat(bottomStart).isEqualTo(0.0f)
            assertThat(ld).isEqualTo(LayoutDirection.Ltr)
        }

        val impl = Impl(
            topStart = CornerSize(10.0f),
            topEnd = CornerSize(10.0f),
            bottomEnd = CornerSize(0f),
            bottomStart = CornerSize(0f),
            onOutlineRequested = assertSizes
        )

        impl.createOutline(sizeWithLargerWidth, LayoutDirection.Ltr, density)
        impl.createOutline(sizeWithLargerHeight, LayoutDirection.Ltr, density)

        assertThat(sizesList).isEqualTo(mutableListOf(sizeWithLargerWidth, sizeWithLargerHeight))
    }

    @Test
    fun topCornersUse100Percent() {
        val density = Density(2f, 1f)
        val sizeWithLargerWidth = Size(6.0f, 4.0f)
        val sizeWithLargerHeight = Size(4.0f, 6.0f)

        val sizesList = mutableListOf<Size>()
        val assertSizes = { size: Size,
            topStart: Float,
            topEnd: Float,
            bottomEnd: Float,
            bottomStart: Float,
            ld: LayoutDirection ->
            sizesList.add(size)
            assertThat(topStart).isEqualTo(4.0f)
            assertThat(topEnd).isEqualTo(4.0f)
            assertThat(bottomEnd).isEqualTo(0.0f)
            assertThat(bottomStart).isEqualTo(0.0f)
            assertThat(ld).isEqualTo(LayoutDirection.Ltr)
        }

        val impl = Impl(
            topStart = CornerSize(100),
            topEnd = CornerSize(100),
            bottomEnd = CornerSize(0),
            bottomStart = CornerSize(0),
            onOutlineRequested = assertSizes
        )

        impl.createOutline(sizeWithLargerWidth, LayoutDirection.Ltr, density)
        impl.createOutline(sizeWithLargerHeight, LayoutDirection.Ltr, density)

        assertThat(sizesList).isEqualTo(mutableListOf(sizeWithLargerWidth, sizeWithLargerHeight))
    }

    @Test
    fun copyingUsesCorrectDefaults() {
        val impl = Impl(
            topStart = CornerSize(4.0f),
            topEnd = CornerSize(3.0f),
            bottomEnd = CornerSize(3.dp),
            bottomStart = CornerSize(50)
        )
        assertThat(impl)
            .isEqualTo(
                impl.copy(
                    bottomEnd = CornerSize(
                        3.dp
                    )
                )
            )
    }

    @Test
    fun layoutDirectionIsPassed() {
        val density = Density(2f, 1f)
        val passedSize = Size(100.0f, 50.0f)
        var assertionExecuted = false
        val assertSizes = {
            size: Size,
            topStart: Float,
            topEnd: Float,
            bottomEnd: Float,
            bottomStart: Float,
            ld: LayoutDirection ->
            assertThat(size).isEqualTo(passedSize)
            assertThat(topStart).isEqualTo(4.0f)
            assertThat(topEnd).isEqualTo(3.0f)
            assertThat(bottomEnd).isEqualTo(6.0f)
            assertThat(bottomStart).isEqualTo(25.0f)
            assertThat(ld).isEqualTo(LayoutDirection.Rtl)
            assertionExecuted = true
        }
        val impl = Impl(
            topStart = CornerSize(4.0f),
            topEnd = CornerSize(3.0f),
            bottomEnd = CornerSize(3.dp),
            bottomStart = CornerSize(50),
            onOutlineRequested = assertSizes
        )

        assertThat(impl.createOutline(passedSize, LayoutDirection.Rtl, density))
            .isEqualTo(Outline.Rectangle(passedSize.toRect()))

        assertThat(assertionExecuted).isTrue()
    }

    @Test
    fun overSizedEqualCornerSizes() {
        val density = Density(2f, 1f)
        val sizeWithLargerWidth = Size(6.0f, 4.0f)
        val sizeWithLargerHeight = Size(4.0f, 6.0f)

        val sizesList = mutableListOf<Size>()
        val assertSizes = { size: Size,
            topStart: Float,
            topEnd: Float,
            bottomEnd: Float,
            bottomStart: Float,
            ld: LayoutDirection ->
            sizesList.add(size)
            assertThat(topStart).isEqualTo(2.0f)
            assertThat(topEnd).isEqualTo(2.0f)
            assertThat(bottomEnd).isEqualTo(2.0f)
            assertThat(bottomStart).isEqualTo(2.0f)
            assertThat(ld).isEqualTo(LayoutDirection.Ltr)
        }

        val impl = Impl(
            topStart = CornerSize(75),
            topEnd = CornerSize(75),
            bottomEnd = CornerSize(75),
            bottomStart = CornerSize(75),
            onOutlineRequested = assertSizes
        )

        impl.createOutline(sizeWithLargerWidth, LayoutDirection.Ltr, density)
        impl.createOutline(sizeWithLargerHeight, LayoutDirection.Ltr, density)

        assertThat(sizesList).isEqualTo(mutableListOf(sizeWithLargerWidth, sizeWithLargerHeight))
    }

    @Test
    fun overSizedCornerSizesShouldProportionallyScale() {
        val density = Density(2f, 1f)
        val sizeWithLargerWidth = Size(15.0f, 10.0f)
        val sizeWithLargerHeight = Size(10.0f, 15.0f)

        val sizesList = mutableListOf<Size>()
        val assertSizes = { size: Size,
            topStart: Float,
            topEnd: Float,
            bottomEnd: Float,
            bottomStart: Float,
            ld: LayoutDirection ->
            sizesList.add(size)
            assertThat(topStart).isEqualTo(7.5f)
            assertThat(topEnd).isEqualTo(2.5f)
            assertThat(bottomEnd).isEqualTo(7.5f)
            assertThat(bottomStart).isEqualTo(2.5f)
            assertThat(ld).isEqualTo(LayoutDirection.Ltr)
        }

        val impl = Impl(
            topStart = CornerSize(90),
            topEnd = CornerSize(30),
            bottomEnd = CornerSize(90),
            bottomStart = CornerSize(30),
            onOutlineRequested = assertSizes
        )

        impl.createOutline(sizeWithLargerWidth, LayoutDirection.Ltr, density)
        impl.createOutline(sizeWithLargerHeight, LayoutDirection.Ltr, density)

        assertThat(sizesList).isEqualTo(mutableListOf(sizeWithLargerWidth, sizeWithLargerHeight))
    }
}

private class Impl(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    private val onOutlineRequested:
        ((Size, Float, Float, Float, Float, LayoutDirection) -> Unit)? = null
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        onOutlineRequested?.invoke(size, topStart, topEnd, bottomEnd, bottomStart, layoutDirection)
        return Outline.Rectangle(size.toRect())
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ) = Impl(topStart, topEnd, bottomEnd, bottomStart, onOutlineRequested)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Impl) return false

        if (topStart != other.topStart) return false
        if (topEnd != other.topEnd) return false
        if (bottomEnd != other.bottomEnd) return false
        if (bottomStart != other.bottomStart) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return result
    }

    override fun toString(): String {
        return "Impl(topStart = $topStart, topEnd = $topEnd, bottomEnd = $bottomEnd, bottomStart" +
            " = $bottomStart)"
    }
}
