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

package androidx.compose.foundation.shape

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RoundedCornerShapeTest {

    private val density = Density(2f)
    private val size = Size(100.0f, 150.0f)

    @Test
    fun roundedUniformCorners() {
        val rounded = RoundedCornerShape(25)

        val expectedRadius = CornerRadius(25f)
        val outline = rounded.toOutline() as Outline.Rounded
        assertThat(outline.roundRect).isEqualTo(
            RoundRect(size.toRect(), expectedRadius)
        )
    }

    @Test
    fun roundedDifferentRadius() {
        val radius1 = 12f
        val radius2 = 22f
        val radius3 = 32f
        val radius4 = 42f
        val rounded = RoundedCornerShape(
            radius1,
            radius2,
            radius3,
            radius4
        )

        val outline = rounded.toOutline() as Outline.Rounded
        assertThat(outline.roundRect).isEqualTo(
            RoundRect(
                size.toRect(),
                CornerRadius(radius1),
                CornerRadius(radius2),
                CornerRadius(radius3),
                CornerRadius(radius4)
            )
        )
    }

    @Test
    fun roundedDifferentRadius_rtl() {
        val radius1 = 12f
        val radius2 = 22f
        val radius3 = 32f
        val radius4 = 42f
        val rounded = RoundedCornerShape(
            radius1,
            radius2,
            radius3,
            radius4
        )

        val outline = rounded.toOutline(LayoutDirection.Rtl) as Outline.Rounded
        assertThat(outline.roundRect).isEqualTo(
            RoundRect(
                size.toRect(),
                CornerRadius(radius2),
                CornerRadius(radius1),
                CornerRadius(radius4),
                CornerRadius(radius3)
            )
        )
    }

    @Test
    fun createsRectangleOutlineForZeroSizedCorners() {
        val rounded = RoundedCornerShape(
            0.0f,
            0.0f,
            0.0f,
            0.0f
        )

        assertThat(rounded.toOutline())
            .isEqualTo(Outline.Rectangle(size.toRect()))
    }

    @Test
    fun roundedCornerShapesAreEquals() {
        assertThat(RoundedCornerShape(12.dp))
            .isEqualTo(RoundedCornerShape(12.dp))
    }

    @Test
    fun roundedCornerUpdateAllCornerSize() {
        assertThat(
            RoundedCornerShape(10.0f).copy(
                CornerSize(
                    5.dp
                )
            )
        )
            .isEqualTo(RoundedCornerShape(5.dp))
    }

    @Test
    fun roundedCornerUpdateTwoCornerSizes() {
        val original = RoundedCornerShape(10.0f)
            .copy(
                topStart = CornerSize(3.dp),
                bottomEnd = CornerSize(50)
            )

        assertEquals(CornerSize(3.dp), original.topStart)
        assertEquals(CornerSize(10.0f), original.topEnd)
        assertEquals(CornerSize(50), original.bottomEnd)
        assertEquals(CornerSize(10.0f), original.bottomStart)
        assertThat(
            RoundedCornerShape(10.0f).copy(
                topStart = CornerSize(3.dp),
                bottomEnd = CornerSize(50)
            )
        ).isEqualTo(
            RoundedCornerShape(
                topStart = CornerSize(3.dp),
                topEnd = CornerSize(10.0f),
                bottomEnd = CornerSize(50),
                bottomStart = CornerSize(10.0f)
            )
        )
    }

    @Test
    fun objectsWithTheSameCornersAreEquals() {
        @Suppress("ReplaceCallWithBinaryOperator")
        assertThat(
            RoundedCornerShape(
                topStart = CornerSize(4.0f),
                topEnd = CornerSize(3.0f),
                bottomEnd = CornerSize(3.dp),
                bottomStart = CornerSize(50)
            ).equals(
                RoundedCornerShape(
                    topStart = CornerSize(4.0f),
                    topEnd = CornerSize(3.0f),
                    bottomEnd = CornerSize(3.dp),
                    bottomStart = CornerSize(50)
                )
            )
        ).isTrue()
    }

    @Test
    fun objectsWithDifferentCornersAreNotEquals() {
        @Suppress("ReplaceCallWithBinaryOperator")
        assertThat(
            RoundedCornerShape(
                topStart = CornerSize(4.0f),
                topEnd = CornerSize(3.0f),
                bottomEnd = CornerSize(3.dp),
                bottomStart = CornerSize(50)
            ).equals(
                RoundedCornerShape(
                    topStart = CornerSize(4.0f),
                    topEnd = CornerSize(5.0f),
                    bottomEnd = CornerSize(3.dp),
                    bottomStart = CornerSize(50)
                )
            )
        ).isFalse()
    }

    @Test
    fun notEqualsToCutCornersWithTheSameSizes() {
        @Suppress("ReplaceCallWithBinaryOperator")
        assertThat(
            RoundedCornerShape(
                topStart = CornerSize(4.0f),
                topEnd = CornerSize(3.0f),
                bottomEnd = CornerSize(3.dp),
                bottomStart = CornerSize(50)
            ).equals(
                CutCornerShape(
                    topStart = CornerSize(4.0f),
                    topEnd = CornerSize(3.0f),
                    bottomEnd = CornerSize(3.dp),
                    bottomStart = CornerSize(50)
                )
            )
        ).isFalse()
    }

    @Test
    fun copyHasCorrectDefaults() {
        assertEquals(
            RoundedCornerShape(
                topStart = 5.dp,
                topEnd = 6.dp,
                bottomEnd = 3.dp,
                bottomStart = 4.dp
            ),
            RoundedCornerShape(
                topStart = 1.dp,
                topEnd = 2.dp,
                bottomEnd = 3.dp,
                bottomStart = 4.dp
            ).copy(topStart = CornerSize(5.dp), topEnd = CornerSize(6.dp))
        )
        assertEquals(
            RoundedCornerShape(
                topStart = 1.dp,
                topEnd = 2.dp,
                bottomEnd = 5.dp,
                bottomStart = 6.dp
            ),
            RoundedCornerShape(
                topStart = 1.dp,
                topEnd = 2.dp,
                bottomEnd = 3.dp,
                bottomStart = 4.dp
            ).copy(bottomEnd = CornerSize(5.dp), bottomStart = CornerSize(6.dp))
        )
    }

    private fun Shape.toOutline(direction: LayoutDirection = LayoutDirection.Ltr) =
        createOutline(size, direction, density)
}
