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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope

@MediumTest
@RunWith(AndroidJUnit4::class)
class AwaitEachGestureTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun awaitEachGestureInternalCancellation() {
        val inputLatch = CountDownLatch(1)
        rule.setContent {
            Box(
                Modifier.pointerInput(Unit) {
                    try {
                        var count = 0
                        coroutineScope {
                            awaitEachGesture {
                                when (count++) {
                                    0 -> Unit // continue
                                    1 -> throw CancellationException("internal exception")
                                    else -> {
                                        // detectGestures will loop infinitely with nothing in the
                                        // middle so wait for cancellation
                                        cancel("really canceled")
                                    }
                                }
                            }
                        }
                    } catch (cancellationException: CancellationException) {
                        assertWithMessage("The internal exception shouldn't cancel detectGestures")
                            .that(cancellationException.message)
                            .isEqualTo("really canceled")
                    }
                    inputLatch.countDown()
                }.size(10.dp)
            )
        }
        rule.waitForIdle()
        assertThat(inputLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun awaitEachGestureLoops() {
        val events = mutableListOf<PointerEventType>()
        val tag = "input rect"
        rule.setContent {
            Box(
                Modifier.fillMaxSize()
                    .testTag(tag)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val event = awaitPointerEvent()
                            events += event.type
                        }
                    }
            )
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset.Zero)
            moveBy(Offset(10f, 10f))
            up()
            down(Offset(3f, 3f))
            moveBy(Offset(10f, 10f))
            moveBy(Offset(1f, 1f))
            up()
        }
        assertThat(events).hasSize(2)
        assertThat(events).containsExactly(PointerEventType.Press, PointerEventType.Press)
    }
}
