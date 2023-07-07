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

import androidx.test.filters.MediumTest
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.channels.Channel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals

@MediumTest
@RunWith(AndroidJUnit4::class)
class ProduceStateTests {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testProducingState() {
        var observedResult = -1
        val emitter = Channel<Int>(Channel.BUFFERED)

        rule.setContent {
            val state by produceState(0, emitter) {
                for (item in emitter) {
                    value = item
                }
            }

            DisposableEffect(state) {
                observedResult = state
                onDispose { }
            }
        }

        assertEquals(0, observedResult, "observedResult after initial composition")

        emitter.trySend(1)
        rule.runOnIdle {
            assertEquals(1, observedResult, "observedResult after emitting new value")
        }
    }
}