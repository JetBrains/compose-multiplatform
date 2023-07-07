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

package androidx.compose.ui.node

import androidx.compose.runtime.collection.mutableVectorOf
import org.junit.Assert
import org.junit.Test

class NestedVectorStackTests {

    @Test
    fun testPushPopOrder() {
        val stack = NestedVectorStack<Int>()
        stack.push(mutableVectorOf(1, 2, 3))
        stack.push(mutableVectorOf(4, 5, 6))
        stack.push(mutableVectorOf())
        stack.push(mutableVectorOf(7))
        stack.push(mutableVectorOf(8, 9))
        val result = buildString {
            while (stack.isNotEmpty()) {
                append(stack.pop())
            }
        }
        Assert.assertEquals("987654321", result)
    }

    @Test
    fun testPopInBetweenPushes() {
        val stack = NestedVectorStack<Int>()
        stack.push(mutableVectorOf(1, 2, 3, 4))
        stack.pop()
        stack.push(mutableVectorOf(4, 5, 6))
        stack.pop()
        stack.pop()
        stack.push(mutableVectorOf())
        stack.push(mutableVectorOf(5, 6, 7))
        stack.push(mutableVectorOf(8, 9))
        val result = buildString {
            while (stack.isNotEmpty()) {
                append(stack.pop())
            }
        }
        Assert.assertEquals("987654321", result)
    }
}