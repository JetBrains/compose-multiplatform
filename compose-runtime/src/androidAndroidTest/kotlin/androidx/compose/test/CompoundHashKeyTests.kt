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

package androidx.compose.test

import androidx.compose.Composable
import androidx.compose.currentComposer
import androidx.compose.invalidate
import androidx.compose.key
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@MediumTest
@RunWith(AndroidJUnit4::class)
class CompoundHashKeyTests : BaseComposeTest() {
    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test // b/157905524
    fun testWithSubCompose() {
        val outerKeys = mutableListOf<Int>()
        val innerKeys = mutableListOf<Int>()
        val invalidates = mutableListOf<() -> Unit>()
        fun invalidateComposition() {
            invalidates.forEach { it() }
            invalidates.clear()
        }
        @Composable
        fun recordHashKeys() {
            invalidates.add(invalidate)
            outerKeys.add(currentComposer.currentCompoundKeyHash)
            subCompose {
                invalidates.add(invalidate)
                innerKeys.add(currentComposer.currentCompoundKeyHash)
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
        }.then {
            assertEquals(2, outerKeys.size)
            assertEquals(2, innerKeys.size)
            assertNotEquals(outerKeys[0], outerKeys[1])
            assertNotEquals(innerKeys[0], innerKeys[1])

            firstOuter.addAll(outerKeys)
            outerKeys.clear()
            firstInner.addAll(innerKeys)
            innerKeys.clear()
            invalidateComposition()
        }.then {
            assertEquals(firstInner, innerKeys)
            assertEquals(firstOuter, outerKeys)
        }
    }
}
