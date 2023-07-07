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

package androidx.compose.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ContextReceiverTests : BaseComposeTest() {

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testDefaultParams() {
        compose {
            val ctx = CtxA()
            with(ctx) {
                composableA(1) { param1, param2, ctxValue ->
                    assertEquals(param1, 1)
                    assertEquals(param2, "Hello")
                    assertEquals(ctxValue, "A")
                }
                composableA(2, "Nice") { param1, param2, ctxValue ->
                    assertEquals(param1, 2)
                    assertEquals(param2, "Nice")
                    assertEquals(ctxValue, "A")
                }
            }
        }
    }

    context(CtxA)
    @Composable fun composableA(
        param1: Int,
        param2: String = "Hello",
        onBodyInvoked: (Int, String, String) -> Unit
    ) {
        onBodyInvoked(param1, param2, getA())
    }

    @Test
    fun testNestedContextDefaultParams() {
        compose {
            val ctx = CtxA()
            with(ctx) {
                composableAB(5) { param1, param2, ctxValue ->
                    assertEquals(param1, 5)
                    assertEquals(param2, "Hello")
                    assertEquals(ctxValue, "B")
                }
            }
        }
    }

    context(CtxA)
    @Composable fun composableAB(
        param1: Int = 1,
        onBodyInvoked: (Int, String, String) -> Unit
    ) {
        val ctx = CtxB()
        with(ctx) {
            composableB(param1 = param1, onBodyInvoked = onBodyInvoked)
        }
    }

    context(CtxA, CtxB)
    @Composable fun composableB(
        param1: Int,
        param2: String = "Hello",
        onBodyInvoked: (Int, String, String) -> Unit
    ) {
        onBodyInvoked(param1, param2, getB())
    }

    // Context Classes
    class CtxA {
        fun getA() = "A"
    }
    class CtxB {
        fun getB() = "B"
    }
}