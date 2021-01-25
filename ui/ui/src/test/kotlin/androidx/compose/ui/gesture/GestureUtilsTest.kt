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

package androidx.compose.ui.gesture

import androidx.compose.ui.input.pointer.down
import androidx.compose.ui.input.pointer.up
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GestureUtilsTest {

    @Test
    fun anyPointersInBounds_1Up_returnsFalse() {
        assertThat(
            listOf(
                down(0, x = 0f, y = 0f).up(100)
            )
                .anyPointersInBounds(IntSize(1, 1))
        ).isFalse()
    }

    @Test
    fun anyPointersInBounds_4OutOfBounds_returnsFalse() {
        assertThat(
            listOf(
                down(0, x = -1f, y = 0f),
                down(1, x = 1f, y = 0f),
                down(2, x = 0f, y = -1f),
                down(3, x = 0f, y = 1f)
            )
                .anyPointersInBounds(IntSize(1, 1))
        ).isFalse()
    }

    @Test
    fun anyPointersInBounds_1InBounds_returnsTrue() {
        assertThat(
            listOf(down(0, x = 0f, y = 0f))
                .anyPointersInBounds(IntSize(1, 1))
        ).isTrue()
    }

    @Test
    fun anyPointersInBounds_5OneInBounds_returnsTrue() {
        assertThat(
            listOf(
                down(0, x = 0f, y = 0f),
                down(1, x = -1f, y = 0f),
                down(2, x = 1f, y = 0f),
                down(3, x = 0f, y = -1f),
                down(4, x = 0f, y = 1f)
            )
                .anyPointersInBounds(IntSize(1, 1))
        ).isTrue()
    }
}