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

package androidx.compose.runtime

import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectNoChanges
import kotlinx.test.IgnoreJsTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CompoundHashKeyTests {
    @Test // b/157905524
    @IgnoreJsTarget
    fun testWithSubCompose() = compositionTest {
        val outerKeys = mutableListOf<Int>()
        val innerKeys = mutableListOf<Int>()
        val invalidates = mutableListOf<RecomposeScope>()
        fun invalidateComposition() {
            invalidates.forEach { it.invalidate() }
            invalidates.clear()
        }
        @Composable
        fun recordHashKeys() {
            invalidates.add(currentRecomposeScope)
            outerKeys.add(currentCompositeKeyHash)
            TestSubcomposition {
                invalidates.add(currentRecomposeScope)
                innerKeys.add(currentCompositeKeyHash)
            }
        }

        val firstOuter = mutableListOf<Int>()
        val firstInner = mutableListOf<Int>()
        compose {
            (0..1).forEach {
                key(it) {
                    recordHashKeys()
                }
            }
        }
        assertEquals(2, outerKeys.size)
        assertEquals(2, innerKeys.size)
        assertNotEquals(outerKeys[0], outerKeys[1])
        assertNotEquals(innerKeys[0], innerKeys[1])

        firstOuter.addAll(outerKeys)
        outerKeys.clear()
        firstInner.addAll(innerKeys)
        innerKeys.clear()
        invalidateComposition()

        expectNoChanges()

        assertEquals(firstInner, innerKeys)
        assertEquals(firstOuter, outerKeys)
    }

    @Test // b/195185633
    fun testEnumKeys() = compositionTest {
        val testClass = EnumTestClass()
        compose {
            testClass.Test()
        }

        val originalKey = testClass.currentKey
        testClass.scope.invalidate()
        advance()

        assertEquals(originalKey, testClass.currentKey)
    }
}

private class EnumTestClass {
    var currentKey = 0
    lateinit var scope: RecomposeScope
    val state = mutableStateOf(0)
    private val config = mutableStateOf(Config.A)

    @Composable
    fun Test() {
        key(config.value) {
            Child()
        }
    }

    @Composable
    private fun Child() {
        scope = currentRecomposeScope
        currentKey = currentCompositeKeyHash
    }

    enum class Config {
        A, B
    }
}
