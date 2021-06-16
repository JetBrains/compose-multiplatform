/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AddRemoveMutableListTest {
    @Test
    fun `add items`() {
        val list = TestList()

        list.add(1)
        list.add(2)
        list.add(3)

        assertThat(list).isEqualTo(listOf(1, 2, 3))
    }

    @Test
    fun `add items to the middle`() {
        val list = TestList(1, 2, 3)

        list.add(0, -1)
        assertThat(list).isEqualTo(listOf(-1, 1, 2, 3))

        list.add(1, 0)
        assertThat(list).isEqualTo(listOf(-1, 0, 1, 2, 3))

        list.add(5, 4)
        assertThat(list).isEqualTo(listOf(-1, 0, 1, 2, 3, 4))
    }

    @Test
    fun `set item`() {
        val list = TestList(1, 2, 3, 4)

        list[1] = 22
        list[0] = 11
        list[2] = 33
        list[3] = 44

        assertThat(list).isEqualTo(listOf(11, 22, 33, 44))
    }
}

// TODO(demin): why there is a compilation error when we use AddRemoveMutableList<Int>() ?

@Suppress("UNCHECKED_CAST")
private class TestList(
    vararg items: Int
) : AddRemoveMutableList<Any>() {
    private val list: MutableList<Any> = items.toMutableList() as MutableList<Any>

    override val size: Int get() = list.size
    override fun get(index: Int): Any = list[index]

    override fun performAdd(element: Any) {
        list.add(element)
    }

    override fun performRemove(index: Int) {
        list.removeAt(index)
    }
}