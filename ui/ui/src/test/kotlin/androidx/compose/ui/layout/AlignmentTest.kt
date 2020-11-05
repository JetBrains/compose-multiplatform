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

package androidx.compose.ui.layout

import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AlignmentTest {
    private val space = IntSize(100, 100)
    private val space1D = 100

    @Test
    fun testAlign_topStart() {
        assertEquals(
            IntOffset(0, 0),
            Alignment.TopStart.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(100, 0),
            Alignment.TopStart.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_topCenter() {
        assertEquals(
            IntOffset(50, 0),
            Alignment.TopCenter.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(50, 0),
            Alignment.TopCenter.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_topEnd() {
        assertEquals(
            IntOffset(100, 0),
            Alignment.TopEnd.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(0, 0),
            Alignment.TopEnd.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_centerStart() {
        assertEquals(
            IntOffset(0, 50),
            Alignment.CenterStart.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(100, 50),
            Alignment.CenterStart.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_center() {
        assertEquals(
            IntOffset(50, 50),
            Alignment.Center.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(50, 50),
            Alignment.Center.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_centerEnd() {
        assertEquals(
            IntOffset(100, 50),
            Alignment.CenterEnd.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(0, 50),
            Alignment.CenterEnd.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_bottomStart() {
        assertEquals(
            IntOffset(0, 100),
            Alignment.BottomStart.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(100, 100),
            Alignment.BottomStart.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_bottomCenter() {
        assertEquals(
            IntOffset(50, 100),
            Alignment.BottomCenter.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(50, 100),
            Alignment.BottomCenter.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_bottomEnd() {
        assertEquals(
            IntOffset(100, 100),
            Alignment.BottomEnd.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(0, 100),
            Alignment.BottomEnd.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_top() {
        assertEquals(
            0,
            Alignment.Top.align(space1D)
        )
    }

    @Test
    fun testAlign_centerVertically() {
        assertEquals(
            50,
            Alignment.CenterVertically.align(space1D)
        )
    }

    @Test
    fun testAlign_bottom() {
        assertEquals(
            100,
            Alignment.Bottom.align(space1D)
        )
    }

    @Test
    fun testAlign_start() {
        assertEquals(
            0,
            Alignment.Start.align(space1D)
        )
        assertEquals(
            100,
            Alignment.Start.align(space1D, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_centerHorizontally() {
        assertEquals(
            50,
            Alignment.CenterHorizontally.align(space1D)
        )
        assertEquals(
            50,
            Alignment.CenterHorizontally.align(space1D, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_end() {
        assertEquals(
            100,
            Alignment.End.align(space1D)
        )
        assertEquals(
            0,
            Alignment.End.align(space1D, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_custom2D() {
        val alignment = BiasAlignment(-0.5f, 0.5f)
        assertEquals(
            IntOffset(25, 75),
            alignment.align(space)
        )
        assertEquals(
            IntOffset(75, 75),
            alignment.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAlign_custom1D() {
        assertEquals(
            75,
            BiasAlignment.Horizontal(0.5f).align(space1D)
        )
        assertEquals(
            25,
            BiasAlignment.Horizontal(0.5f).align(space1D, LayoutDirection.Rtl)
        )
        assertEquals(
            25,
            BiasAlignment.Vertical(-0.5f).align(space1D)
        )
    }

    @Test
    fun testAbsoluteAlign_left() {
        assertEquals(
            0,
            AbsoluteAlignment.Left.align(space1D)
        )
        assertEquals(
            0,
            AbsoluteAlignment.Left.align(space1D, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_right() {
        assertEquals(
            100,
            AbsoluteAlignment.Right.align(space1D)
        )
        assertEquals(
            100,
            AbsoluteAlignment.Right.align(space1D, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_topLeft() {
        assertEquals(
            IntOffset(0, 0),
            AbsoluteAlignment.TopLeft.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(0, 0),
            AbsoluteAlignment.TopLeft.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_topRight() {
        assertEquals(
            IntOffset(100, 0),
            AbsoluteAlignment.TopRight.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(100, 0),
            AbsoluteAlignment.TopRight.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_centerLeft() {
        assertEquals(
            IntOffset(0, 50),
            AbsoluteAlignment.CenterLeft.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(0, 50),
            AbsoluteAlignment.CenterLeft.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_centerRight() {
        assertEquals(
            IntOffset(100, 50),
            AbsoluteAlignment.CenterRight.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(100, 50),
            AbsoluteAlignment.CenterRight.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_bottomLeft() {
        assertEquals(
            IntOffset(0, 100),
            AbsoluteAlignment.BottomLeft.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(0, 100),
            AbsoluteAlignment.BottomLeft.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_bottomRight() {
        assertEquals(
            IntOffset(100, 100),
            AbsoluteAlignment.BottomRight.align(space, LayoutDirection.Ltr)
        )
        assertEquals(
            IntOffset(100, 100),
            AbsoluteAlignment.BottomRight.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_custom2D() {
        val alignment = BiasAbsoluteAlignment(-0.5f, 0.5f)
        assertEquals(
            IntOffset(25, 75),
            alignment.align(space)
        )
        assertEquals(
            IntOffset(25, 75),
            alignment.align(space, LayoutDirection.Rtl)
        )
    }

    @Test
    fun testAbsoluteAlign_custom1D() {
        assertEquals(
            75,
            BiasAbsoluteAlignment.Horizontal(0.5f).align(space1D)
        )
        assertEquals(
            75,
            BiasAbsoluteAlignment.Horizontal(0.5f).align(space1D, LayoutDirection.Rtl)
        )
    }
}