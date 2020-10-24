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

package androidx.compose.ui.window

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PopupPositionProviderTest {

    @Test
    fun positionTopStart() {
        /* Expected TopStart Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPosition = IntOffset(60, 60)

        assertThat(calculatePosition(Alignment.TopStart, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionTopStart_rtl() {
        /* Expected TopStart Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPosition = IntOffset(100, 60)

        assertThat(calculatePosition(Alignment.TopStart, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionTopCenter() {
        /* Expected TopCenter Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPosition = IntOffset(90, 60)

        assertThat(calculatePosition(Alignment.TopCenter, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionTopCenter_rtl() {
        /* Expected TopCenter Position
           x = parentGlobalPosition.x + (-offset.x) + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPosition = IntOffset(70, 60)

        assertThat(calculatePosition(Alignment.TopCenter, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionTopEnd() {
        /* Expected TopEnd Position
           x = parentGlobalPosition.x + offset.x + parentSize.x - popupSize.x
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPosition = IntOffset(120, 60)

        assertThat(calculatePosition(Alignment.TopEnd, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionTopEnd_rtl() {
        /* Expected TopEnd Position
           x = parentGlobalPosition.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPosition = IntOffset(40, 60)

        assertThat(calculatePosition(Alignment.TopEnd, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionCenterEnd() {
        /* Expected CenterEnd Position
           x = parentGlobalPosition.x + offset.x + parentSize.x - popupSize.x
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(120, 100)

        assertThat(calculatePosition(Alignment.CenterEnd, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionCenterEnd_rtl() {
        /* Expected CenterEnd Position
           x = parentGlobalPosition.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(40, 100)

        assertThat(calculatePosition(Alignment.CenterEnd, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionBottomEnd() {
        /* Expected BottomEnd Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(120, 140)

        assertThat(calculatePosition(Alignment.BottomEnd, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionBottomEnd_rtl() {
        /* Expected BottomEnd Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(40, 140)

        assertThat(calculatePosition(Alignment.BottomEnd, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionBottomCenter() {
        /* Expected BottomCenter Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(90, 140)

        assertThat(calculatePosition(Alignment.BottomCenter, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionBottomCenter_rtl() {
        /* Expected BottomCenter Position
           x = parentGlobalPosition.x + (-offset.x) + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(70, 140)

        assertThat(calculatePosition(Alignment.BottomCenter, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionBottomStart() {
        /* Expected BottomStart Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(60, 140)

        assertThat(calculatePosition(Alignment.BottomStart, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionBottomStart_rtl() {
        /* Expected BottomStart Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(100, 140)

        assertThat(calculatePosition(Alignment.BottomStart, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionCenterStart() {
        /* Expected CenterStart Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(60, 100)

        assertThat(calculatePosition(Alignment.CenterStart, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionCenterStart_rtl() {
        /* Expected CenterStart Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(100, 100)

        assertThat(calculatePosition(Alignment.CenterStart, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionCenter() {
        /* Expected Center Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(90, 100)

        assertThat(calculatePosition(Alignment.Center, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun positionCenter_rtl() {
        /* Expected Center Position
           x = parentGlobalPosition.x + (-offset.x) + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(70, 100)

        assertThat(calculatePosition(Alignment.Center, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun dropdown_positionStart() {
        /* Expected Dropdown Start Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPosition = IntOffset(60, 160)

        assertThat(calculateDropdownPosition(DropDownAlignment.Start, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun dropdown_positionStart_rtl() {
        /* Expected Dropdown Start Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPosition = IntOffset(100, 160)

        assertThat(calculateDropdownPosition(DropDownAlignment.Start, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun dropdown_positionEnd() {
        /* Expected Dropdown End Position
           x = parentGlobalPosition.x + offset.x + parentSize.x
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPosition = IntOffset(160, 160)

        assertThat(calculateDropdownPosition(DropDownAlignment.End, LayoutDirection.Ltr))
            .isEqualTo(expectedPosition)
    }

    @Test
    fun dropdown_positionEnd_rtl() {
        /* Expected Dropdown End Position
           x = parentGlobalPosition.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPosition = IntOffset(0, 160)

        assertThat(calculateDropdownPosition(DropDownAlignment.End, LayoutDirection.Rtl))
            .isEqualTo(expectedPosition)
    }

    private fun calculatePosition(alignment: Alignment, layoutDir: LayoutDirection): IntOffset {
        val parentBounds = IntBounds(50, 50, 150, 150)
        val windowBounds = IntBounds(0, 0, 1000, 1000)
        val offset = IntOffset(10, 10)
        val popupSize = IntSize(40, 20)

        return AlignmentOffsetPositionProvider(alignment, offset)
            .calculatePosition(
                parentBounds,
                windowBounds,
                layoutDir,
                popupSize
            )
    }

    private fun calculateDropdownPosition(
        alignment: DropDownAlignment,
        layoutDir: LayoutDirection
    ): IntOffset {
        val parentBounds = IntBounds(50, 50, 150, 150)
        val windowBounds = IntBounds(0, 0, 1000, 1000)
        val offset = IntOffset(10, 10)
        val popupSize = IntSize(40, 20)

        return DropdownPositionProvider(alignment, offset)
            .calculatePosition(
                parentBounds,
                windowBounds,
                layoutDir,
                popupSize
            )
    }
}