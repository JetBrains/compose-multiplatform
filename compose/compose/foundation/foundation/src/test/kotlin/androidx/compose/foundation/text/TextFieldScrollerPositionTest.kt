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

package androidx.compose.foundation.text

import androidx.compose.foundation.gestures.Orientation
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextFieldScrollerPositionTest {

    private val scrollerPosition = TextFieldScrollerPosition(
        initial = 15f,
        initialOrientation = Orientation.Vertical
    )
    private val ContainerSize = 20

    // container is smaller than cursor
    private val SmallContainerSize = 8

    @Test
    fun coerceOffset_topBottomInvisible_underContainer() {
        scrollerPosition.coerceOffset(
            cursorStart = 0f,
            cursorEnd = 10f,
            containerSize = ContainerSize
        )
        assertThat(scrollerPosition.offset).isEqualTo(0f)
    }

    @Test
    fun coerceOffset_topInvisible_bottomVisible() {
        scrollerPosition.coerceOffset(
            cursorStart = 10f,
            cursorEnd = 20f,
            containerSize = ContainerSize
        )
        assertThat(scrollerPosition.offset).isEqualTo(10f)
    }

    @Test
    fun coerceOffset_topBottomVisible() {
        val initialOffset = scrollerPosition.offset
        scrollerPosition.coerceOffset(
            cursorStart = 20f,
            cursorEnd = 30f,
            containerSize = ContainerSize
        )
        assertThat(scrollerPosition.offset).isEqualTo(initialOffset)
    }

    @Test
    fun coerceOffset_topVisibleBottomInvisible() {
        scrollerPosition.coerceOffset(
            cursorStart = 30f,
            cursorEnd = 40f,
            containerSize = ContainerSize
        )
        assertThat(scrollerPosition.offset).isEqualTo(20f)
    }

    @Test
    fun coerceOffset_topBottomInvisible_belowContainer() {
        scrollerPosition.coerceOffset(
            cursorStart = 40f,
            cursorEnd = 50f,
            containerSize = ContainerSize
        )
        assertThat(scrollerPosition.offset).isEqualTo(30f)
    }

    @Test
    fun coerceOffset_smallContainer_topBottomInvisible_underContainer() {
        scrollerPosition.coerceOffset(
            cursorStart = 0f,
            cursorEnd = 10f,
            containerSize = SmallContainerSize
        )
        assertThat(scrollerPosition.offset).isEqualTo(2f)
    }

    @Test
    fun coerceOffset_smallContainer_topInvisible_bottomVisible() {
        scrollerPosition.coerceOffset(
            cursorStart = 10f,
            cursorEnd = 20f,
            containerSize = SmallContainerSize
        )
        assertThat(scrollerPosition.offset).isEqualTo(12f)
    }

    @Test
    fun coerceOffset_smallContainer_inBetweenTopBottom() {
        scrollerPosition.coerceOffset(
            cursorStart = 14f,
            cursorEnd = 24f,
            containerSize = SmallContainerSize
        )
        assertThat(scrollerPosition.offset).isEqualTo(16f)
    }

    @Test
    fun coerceOffset_smallContainer_topVisibleBottomInvisible() {
        scrollerPosition.coerceOffset(
            cursorStart = 20f,
            cursorEnd = 30f,
            containerSize = SmallContainerSize
        )
        assertThat(scrollerPosition.offset).isEqualTo(22f)
    }

    @Test
    fun coerceOffset_topBottomInvisible_smallContainer_belowContainer() {
        scrollerPosition.coerceOffset(
            cursorStart = 30f,
            cursorEnd = 40f,
            containerSize = SmallContainerSize
        )
        assertThat(scrollerPosition.offset).isEqualTo(32f)
    }
}
