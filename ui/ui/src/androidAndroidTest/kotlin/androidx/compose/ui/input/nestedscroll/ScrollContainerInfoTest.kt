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

import androidx.compose.ui.input.ScrollContainerInfo
import androidx.compose.ui.input.canScroll
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ScrollContainerInfoTest {

    @Test
    fun canScroll_horizontal() {
        val subject = Subject(horizontal = true)

        assertThat(subject.canScroll()).isTrue()
    }

    @Test
    fun canScroll_vertical() {
        val subject = Subject(vertical = true)

        assertThat(subject.canScroll()).isTrue()
    }

    @Test
    fun canScroll_both() {
        val subject = Subject(horizontal = true, vertical = true)

        assertThat(subject.canScroll()).isTrue()
    }

    @Test
    fun canScroll_neither() {
        val subject = Subject(horizontal = false, vertical = false)

        assertThat(subject.canScroll()).isFalse()
    }

    class Subject(
        private val horizontal: Boolean = false,
        private val vertical: Boolean = false,
    ) : ScrollContainerInfo {
        override fun canScrollHorizontally(): Boolean = horizontal
        override fun canScrollVertically(): Boolean = vertical
    }
}
