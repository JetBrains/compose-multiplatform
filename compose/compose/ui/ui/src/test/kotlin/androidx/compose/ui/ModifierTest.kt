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

package androidx.compose.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ModifierTest {
    /**
     * Verifies that the [Modifier.plus] operation results in [Modifier] instances that
     * [Modifier.foldIn] and [Modifier.foldOut] in the expected order.
     */
    @Test
    fun wrapElementChain() {
        val chain = SampleModifier(1) then SampleModifier(2) then SampleModifier(3)
        val forwards = chain.foldIn(emptyList<Int>()) { acc, e ->
            acc + (e as SampleModifier).value
        }
        val backwards = chain.foldOut(emptyList<Int>()) { e, acc ->
            acc + (e as SampleModifier).value
        }
        assertEquals("1-3 folded in (forwards)", listOf(1, 2, 3), forwards)
        assertEquals("1-3 folded out (backwards)", listOf(3, 2, 1), backwards)
    }
}

private class SampleModifier(val value: Int) : Modifier.Element
