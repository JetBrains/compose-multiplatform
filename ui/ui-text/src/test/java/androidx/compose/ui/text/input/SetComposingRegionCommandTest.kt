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

package androidx.compose.ui.text.input

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SetComposingRegionCommandTest {

    @Test
    fun test_set() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetComposingRegionCommand(1, 4).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(4)
    }

    @Test
    fun test_preserve_ongoing_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 3)

        SetComposingRegionCommand(2, 4).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(4)
    }

    @Test
    fun test_preserve_selection() {
        val eb = EditingBuffer("ABCDE", TextRange(1, 4))

        SetComposingRegionCommand(2, 4).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(4)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(4)
    }

    @Test
    fun test_set_reversed() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetComposingRegionCommand(4, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(4)
    }

    @Test
    fun test_set_too_small() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetComposingRegionCommand(-1000, -1000).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_set_too_large() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetComposingRegionCommand(1000, 1000).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_set_too_small_and_too_large() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetComposingRegionCommand(-1000, 1000).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(5)
    }

    @Test
    fun test_set_too_small_and_too_large_reversed() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetComposingRegionCommand(1000, -1000).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(5)
    }
}