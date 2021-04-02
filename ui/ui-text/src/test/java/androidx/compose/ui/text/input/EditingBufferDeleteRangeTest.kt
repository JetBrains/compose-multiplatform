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
class EditingBufferDeleteRangeTest {

    @Test
    fun test_does_not_intersect_deleted_is_after_the_target() {
        val target = TextRange(0, 1)
        val deleted = TextRange(2, 3)
        assertThat(updateRangeAfterDelete(target, deleted))
            .isEqualTo(TextRange(target.start, target.end))
    }

    @Test
    fun test_does_not_intersect_deleted_is_before_the_target() {
        val target = TextRange(4, 5)
        val deleted = TextRange(0, 2)
        assertThat(updateRangeAfterDelete(target, deleted)).isEqualTo(TextRange(2, 3))
    }

    @Test
    fun test_deleted_covers_target() {
        val target = TextRange(1, 2)
        val deleted = TextRange(0, 3)
        assertThat(updateRangeAfterDelete(target, deleted)).isEqualTo(TextRange(0, 0))
    }

    @Test
    fun test_target_covers_deleted() {
        val target = TextRange(0, 3)
        val deleted = TextRange(1, 2)
        assertThat(updateRangeAfterDelete(target, deleted)).isEqualTo(TextRange(0, 2))
    }

    @Test
    fun test_deleted_same_as_target() {
        val target = TextRange(1, 2)
        val deleted = TextRange(1, 2)
        assertThat(updateRangeAfterDelete(target, deleted)).isEqualTo(TextRange(1, 1))
    }

    @Test
    fun test_deleted_covers_first_half_of_target() {
        val target = TextRange(1, 4)
        val deleted = TextRange(0, 2)
        assertThat(updateRangeAfterDelete(target, deleted)).isEqualTo(TextRange(0, 2))
    }

    @Test
    fun test_deleted_covers_second_half_of_target() {
        val target = TextRange(1, 4)
        val deleted = TextRange(3, 5)
        assertThat(updateRangeAfterDelete(target, deleted)).isEqualTo(TextRange(1, 3))
    }

    @Test
    fun test_delete_trailing_cursor() {
        val target = TextRange(3, 3)
        val deleted = TextRange(1, 2)
        assertThat(updateRangeAfterDelete(target, deleted)).isEqualTo(TextRange(2, 2))
    }
}
