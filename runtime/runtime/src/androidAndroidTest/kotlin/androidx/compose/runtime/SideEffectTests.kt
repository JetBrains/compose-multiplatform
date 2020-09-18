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

package androidx.compose.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("UNUSED_VARIABLE")
@MediumTest
@RunWith(AndroidJUnit4::class)
class SideEffectTests : BaseComposeTest() {

    @get:Rule
    override val activityRule = makeTestActivityRule()

    /**
     * Test that side effects run in order of appearance each time the composable
     * is recomposed.
     */
    @Test
    fun testSideEffectsRunInOrder() {
        val results = mutableListOf<Int>()
        var resultsAtComposition: List<Int>? = null
        var recompose: (() -> Unit)? = null
        compose {
            SideEffect {
                results += 1
            }
            SideEffect {
                results += 2
            }
            resultsAtComposition = results.toList()
            recompose = invalidate
        }.then {
            assertEquals(listOf(1, 2), results, "side effects were applied")
            assertEquals(
                emptyList(), resultsAtComposition,
                "side effects weren't applied until after composition"
            )
            recompose?.invoke() ?: error("missing recompose function")
        }.then {
            assertEquals(listOf(1, 2, 1, 2), results, "side effects applied a second time")
        }
    }

    /**
     * Test that side effects run after lifecycle observers enter the composition,
     * even if their remembrance happens after the SideEffect call appears.
     */
    @Test
    fun testSideEffectsRunAfterLifecycleObservers() {
        class MyObserver : CompositionLifecycleObserver {
            var isPresent: Boolean = false
                private set

            override fun onEnter() {
                isPresent = true
            }

            override fun onLeave() {
                isPresent = false
            }
        }

        val myObserverOne = MyObserver()
        val myObserverTwo = MyObserver()
        var wasObserverOnePresent = false
        var wasObserverTwoPresent = false

        compose {
            val one = remember { myObserverOne }
            SideEffect {
                wasObserverOnePresent = myObserverOne.isPresent
                wasObserverTwoPresent = myObserverTwo.isPresent
            }
            val two = remember { myObserverTwo }
        }.then {
            assertTrue(wasObserverOnePresent, "observer one present for side effect")
            assertTrue(wasObserverTwoPresent, "observer two present for side effect")
        }
    }
}
