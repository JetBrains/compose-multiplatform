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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class AbsoluteCutCornerShapeTest(val layoutDirection: LayoutDirection) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<LayoutDirection> = arrayOf(
            LayoutDirection.Ltr,
            LayoutDirection.Rtl
        )
    }

    private val density = Density(2f)
    private val size = Size(100.0f, 150.0f)

    @Test
    fun cutCornersUniformCorners() {
        val cut = AbsoluteCutCornerShape(10.0f)

        val outline = cut.toOutline() as Outline.Generic
        assertPathsEquals(
            outline.path,
            Path().apply {
                moveTo(0f, 10f)
                lineTo(10f, 0f)
                lineTo(90f, 0f)
                lineTo(100f, 10f)
                lineTo(100f, 140f)
                lineTo(90f, 150f)
                lineTo(10f, 150f)
                lineTo(0f, 140f)
                close()
            }
        )
    }

    @Test
    fun cutCornersDifferentCorners() {
        val size1 = 12f
        val size2 = 22f
        val size3 = 32f
        val size4 = 42f
        val cut = AbsoluteCutCornerShape(
            size1,
            size2,
            size3,
            size4
        )

        val outline = cut.toOutline() as Outline.Generic
        assertPathsEquals(
            outline.path,
            Path().apply {
                moveTo(0f, size1)
                lineTo(size1, 0f)
                lineTo(size.width - size2, 0f)
                lineTo(size.width, size2)
                lineTo(size.width, size.height - size3)
                lineTo(size.width - size3, size.height)
                lineTo(size4, size.height)
                lineTo(0f, size.height - size4)
                close()
            }
        )
    }

    @Test
    fun createsRectangleOutlineForZeroSizedCorners() {
        val rounded = AbsoluteCutCornerShape(
            0.0f,
            0.0f,
            0.0f,
            0.0f
        )

        assertThat(rounded.toOutline())
            .isEqualTo(Outline.Rectangle(size.toRect()))
    }

    @Test
    fun cutCornerShapesAreEquals() {
        assertThat(AbsoluteCutCornerShape(10.0f))
            .isEqualTo(AbsoluteCutCornerShape(10.0f))
    }

    @Test
    fun cutCornerUpdateAllCornerSize() {
        assertThat(
            AbsoluteCutCornerShape(10.0f).copy(
                CornerSize(
                    5.0f
                )
            )
        )
            .isEqualTo(AbsoluteCutCornerShape(5.0f))
    }

    @Test
    fun cutCornerUpdateTwoCornerSizes() {
        assertThat(
            AbsoluteCutCornerShape(10.0f).copy(
                topEnd = CornerSize(3.dp),
                bottomEnd = CornerSize(50)
            )
        ).isEqualTo(
            AbsoluteCutCornerShape(
                topLeft = CornerSize(10.0f),
                topRight = CornerSize(3.dp),
                bottomLeft = CornerSize(10.0f),
                bottomRight = CornerSize(50)
            )
        )
    }

    @Test
    fun objectsWithTheSameCornersAreEquals() {
        @Suppress("ReplaceCallWithBinaryOperator")
        assertThat(
            AbsoluteCutCornerShape(
                topLeft = CornerSize(4.0f),
                topRight = CornerSize(3.0f),
                bottomLeft = CornerSize(3.dp),
                bottomRight = CornerSize(50)
            ).equals(
                AbsoluteCutCornerShape(
                    topLeft = CornerSize(4.0f),
                    topRight = CornerSize(3.0f),
                    bottomLeft = CornerSize(3.dp),
                    bottomRight = CornerSize(50)
                )
            )
        ).isTrue()
    }

    @Test
    fun objectsWithDifferentCornersAreNotEquals() {
        @Suppress("ReplaceCallWithBinaryOperator")
        assertThat(
            AbsoluteCutCornerShape(
                topLeft = CornerSize(4.0f),
                topRight = CornerSize(3.0f),
                bottomLeft = CornerSize(3.dp),
                bottomRight = CornerSize(50)
            ).equals(
                AbsoluteCutCornerShape(
                    topLeft = CornerSize(4.0f),
                    topRight = CornerSize(5.0f),
                    bottomLeft = CornerSize(3.dp),
                    bottomRight = CornerSize(50)
                )
            )
        ).isFalse()
    }

    @Test
    fun copyHasCorrectDefaults() {
        assertEquals(
            AbsoluteCutCornerShape(
                topLeft = 5.dp,
                topRight = 6.dp,
                bottomRight = 3.dp,
                bottomLeft = 4.dp
            ),
            AbsoluteCutCornerShape(
                topLeft = 1.dp,
                topRight = 2.dp,
                bottomRight = 3.dp,
                bottomLeft = 4.dp
            ).copy(topStart = CornerSize(5.dp), topEnd = CornerSize(6.dp))
        )
        assertEquals(
            AbsoluteCutCornerShape(
                topLeft = 1.dp,
                topRight = 2.dp,
                bottomRight = 5.dp,
                bottomLeft = 6.dp
            ),
            AbsoluteCutCornerShape(
                topLeft = 1.dp,
                topRight = 2.dp,
                bottomRight = 3.dp,
                bottomLeft = 4.dp
            ).copy(bottomEnd = CornerSize(5.dp), bottomStart = CornerSize(6.dp))
        )
    }

    private fun Shape.toOutline() =
        createOutline(size, layoutDirection, density)
}
