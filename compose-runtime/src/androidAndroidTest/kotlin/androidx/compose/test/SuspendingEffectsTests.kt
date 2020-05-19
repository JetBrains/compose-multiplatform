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

import androidx.compose.Choreographer
import androidx.compose.ChoreographerFrameCallback
import androidx.compose.clearRoots
import androidx.compose.getValue
import androidx.compose.launchInComposition
import androidx.compose.mutableStateOf
import androidx.compose.onPreCommit
import androidx.compose.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.channels.Channel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@MediumTest
@RunWith(AndroidJUnit4::class)
class SuspendingEffectsTests : BaseComposeTest() {

    @After
    fun teardown() {
        clearRoots()
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testLaunchInComposition() {
        var counter by mutableStateOf(0)

        // Used as a signal that launchInComposition will await
        val ch = Channel<Unit>(Channel.CONFLATED)
        compose {
            launchInComposition {
                counter++
                ch.receive()
                counter++
                ch.receive()
                counter++
            }
        }.then {
            assertEquals(1, counter)
            ch.offer(Unit)
        }.then {
            assertEquals(2, counter)
            ch.offer(Unit)
        }.then {
            assertEquals(3, counter)
        }
    }

    @Test
    fun testAwaitFrame() {
        var choreographerTime by mutableStateOf(Long.MIN_VALUE)
        var awaitFrameTime by mutableStateOf(Long.MAX_VALUE)
        compose {
            launchInComposition {
                awaitFrameNanos {
                    awaitFrameTime = it
                }
            }
            onPreCommit(true) {
                Choreographer.postFrameCallback(object : ChoreographerFrameCallback {
                    override fun doFrame(frameTimeNanos: Long) {
                        choreographerTime = frameTimeNanos
                    }
                })
            }
        }.then {
            assertEquals(choreographerTime, awaitFrameTime,
                "expected same values from choreographer post and awaitFrameNanos")
        }
    }
}