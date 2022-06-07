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

package androidx.compose.material

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import org.junit.runners.JUnit4
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(JUnit4::class)
class DesktopMenuTest {

    val windowSize = IntSize(100, 100)
    val anchorPosition = IntOffset(10, 10)
    val anchorSize = IntSize(80, 20)

    @Test
    fun menu_positioning_vertical_underAnchor() {
        val popupSize = IntSize(80, 70)

        val position = DesktopDropdownMenuPositionProvider(
            DpOffset.Zero,
            Density(1f)
        ).calculatePosition(
            IntRect(anchorPosition, anchorSize),
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(position).isEqualTo(IntOffset(10, 30))
    }

    @Test
    fun menu_positioning_vertical_windowTop() {
        val popupSize = IntSize(80, 100)

        val position = DesktopDropdownMenuPositionProvider(
            DpOffset.Zero,
            Density(1f)
        ).calculatePosition(
            IntRect(anchorPosition, anchorSize),
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(position).isEqualTo(IntOffset(10, 0))
    }
}