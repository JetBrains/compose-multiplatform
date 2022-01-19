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

package androidx.compose.foundation.gesture

import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@MediumTest
@RunWith(AndroidJUnit4::class)
class ForEachGestureTest {
    @get:Rule
    val rule = createComposeRule()

    /**
     * Make sure that an empty `forEachGesture` block does not cause a crash.
     */
    @Test
    fun testEmptyForEachGesture() {
        val latch1 = CountDownLatch(2)
        val latch2 = CountDownLatch(1)
        rule.setContent {
            Box(
                Modifier.pointerInput(Unit) {
                    forEachGesture {
                        if (latch1.count == 0L) {
                            // forEachGesture will loop infinitely with nothing in the middle
                            // so wait for cancellation
                            awaitCancellation()
                        }
                        latch1.countDown()
                    }
                }.pointerInput(Unit) {
                    awaitPointerEventScope {
                        assertTrue(currentEvent.changes.isEmpty())
                        latch2.countDown()
                    }
                }.size(10.dp)
            )
        }
        assertTrue(latch1.await(1, TimeUnit.SECONDS))
        assertTrue(latch2.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testForEachGestureInternalCancellation() {
        rule.setContent {
            Box(
                Modifier.pointerInput(Unit) {
                    try {
                        var count = 0

                        forEachGesture {
                            when (count++) {
                                0 -> Unit // continue
                                1 -> throw CancellationException("internal exception")
                                else -> {
                                    // forEachGesture will loop infinitely with nothing in the
                                    // middle so wait for cancellation
                                    awaitCancellation()
                                }
                            }
                        }
                    } catch (cancellationException: CancellationException) {
                        assertWithMessage("The internal exception shouldn't cancel forEachGesture")
                            .that(cancellationException.message)
                            .isNotEqualTo("internal exception")
                    }
                }.size(10.dp)
            )
        }
    }

    @Test
    fun testForEachGestureExternalCancellation() {
        rule.setContent {
            Box(
                Modifier.pointerInput(Unit) {
                    coroutineScope {
                        val job = launch(Dispatchers.Unconfined) {
                            forEachGesture {
                                // forEachGesture will loop infinitely with nothing in the middle
                                // so wait for cancellation
                                awaitCancellation()
                            }

                            assertWithMessage("forEachGesture should have been cancelled").fail()
                        }

                        job.cancel()
                    }
                }.size(10.dp)
            )
        }
    }
}
