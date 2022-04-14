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

package androidx.compose.ui.input.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.setFocusableContent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.input.rotary.RotaryScrollEvent as FocusAwareTestEvent
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent as onPreFocusAwareEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent as onFocusAwareEvent
import androidx.compose.ui.test.performRotaryScrollInput as performFocusAwareInput

/**
 * Focus-aware event propagation test.
 *
 * This test verifies the event propagation logic using
 * [androidx.compose.ui.input.rotary.RotaryScrollEvent]s, but it is meant to test the generic
 * propagation logic for all focus-aware events.
 */
@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusAwareEventPropagationTest {
    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalComposeUiApi::class)
    private val sentEvent: FocusAwareTestEvent =
        FocusAwareTestEvent(1f, 2f, 3L)
    private var receivedEvent: FocusAwareTestEvent? = null
    private val initialFocus = FocusRequester()

    @Test
    fun noFocusable_doesNotDeliverEvent() {
        // Arrange.
        rule.setContent {
            Box(
                modifier = Modifier.onFocusAwareEvent {
                    receivedEvent = it
                    true
                }
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        assertThat(receivedEvent).isNull()
    }

    @Test
    fun unfocusedFocusable_doesNotDeliverEvent() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusAwareEvent {
                        receivedEvent = it
                        true
                    }
                    .focusable()
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        assertThat(receivedEvent).isNull()
    }

    @Test
    fun onFocusAwareEvent_afterFocusable_isNotTriggered() {
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .focusable(initiallyFocused = true)
                    .onFocusAwareEvent {
                        receivedEvent = it
                        true
                    }

            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        assertThat(receivedEvent).isNull()
    }

    @Test
    fun onPreFocusAwareEvent_afterFocusable_isNotTriggered() {
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .focusable(initiallyFocused = true)
                    .onPreFocusAwareEvent {
                        receivedEvent = it
                        true
                    }
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        assertThat(receivedEvent).isNull()
    }

    @Test
    fun onFocusAwareEvent_isTriggered() {
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onFocusAwareEvent {
                        receivedEvent = it
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            // performFocusAwareInput generates a vertical scroll
            assertThat(sentEvent.verticalScrollPixels)
                .isEqualTo(receivedEvent?.verticalScrollPixels)
        }
    }

    @Test
    fun onPreviewKeyEvent_triggered() {
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onPreFocusAwareEvent {
                        receivedEvent = it
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            // performFocusAwareInput generates a vertical scroll
            assertThat(sentEvent.verticalScrollPixels)
                .isEqualTo(receivedEvent?.verticalScrollPixels)
        }
    }

    @Test
    fun onFocusAwareEventNotTriggered_ifOnPreFocusAwareEventConsumesEvent_1() {
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onFocusAwareEvent {
                        receivedEvent = it
                        true
                    }
                    .onPreFocusAwareEvent {
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(receivedEvent).isNull()
        }
    }

    @Test
    fun onFocusAwareEventNotTriggered_ifOnPreFocusAwareEventConsumesEvent_2() {
        // Arrange.
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onPreFocusAwareEvent {
                        true
                    }
                    .onFocusAwareEvent {
                        receivedEvent = it
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(receivedEvent).isNull()
        }
    }

    @Test
    fun onPreFocusAwareEvent_triggeredBefore_onFocusAwareEvent_1() {
        // Arrange.
        var triggerIndex = 1
        var onFocusAwareEventTrigger = 0
        var onPreFocusAwareEventTrigger = 0
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onFocusAwareEvent {
                        onFocusAwareEventTrigger = triggerIndex++
                        true
                    }
                    .onPreFocusAwareEvent {
                        onPreFocusAwareEventTrigger = triggerIndex++
                        false
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(onPreFocusAwareEventTrigger).isEqualTo(1)
            assertThat(onFocusAwareEventTrigger).isEqualTo(2)
        }
    }

    @Test
    fun onPreFocusAwareEvent_triggeredBefore_onFocusAwareEvent_2() {
        // Arrange.
        var triggerIndex = 1
        var onFocusAwareEventTrigger = 0
        var onPreFocusAwareEventTrigger = 0
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onPreFocusAwareEvent {
                        onPreFocusAwareEventTrigger = triggerIndex++
                        false
                    }
                    .onFocusAwareEvent {
                        onFocusAwareEventTrigger = triggerIndex++
                        true
                    }
                    .focusable(initiallyFocused = true)
            )
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(onPreFocusAwareEventTrigger).isEqualTo(1)
            assertThat(onFocusAwareEventTrigger).isEqualTo(2)
        }
    }

    @Test
    fun parent_child() {
        // Arrange.
        var triggerIndex = 1
        var parentOnFocusAwareEventTrigger = 0
        var parentOnPreFocusAwareEventTrigger = 0
        var childOnFocusAwareEventTrigger = 0
        var childOnPreFocusAwareEventTrigger = 0
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onFocusAwareEvent {
                        parentOnFocusAwareEventTrigger = triggerIndex++
                        false
                    }
                    .onPreFocusAwareEvent {
                        parentOnPreFocusAwareEventTrigger = triggerIndex++
                        false
                    }
                    .focusable()
            ) {
                Box(
                    modifier = Modifier
                        .onFocusAwareEvent {
                            childOnFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .onPreFocusAwareEvent {
                            childOnPreFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .focusable(initiallyFocused = true)
                )
            }
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(parentOnPreFocusAwareEventTrigger).isEqualTo(1)
            assertThat(childOnPreFocusAwareEventTrigger).isEqualTo(2)
            assertThat(childOnFocusAwareEventTrigger).isEqualTo(3)
            assertThat(parentOnFocusAwareEventTrigger).isEqualTo(4)
        }
    }

    @Test
    fun parent_child_noFocusModifierForParent() {
        // Arrange.
        var triggerIndex = 1
        var parentOnFocusAwareEventTrigger = 0
        var parentOnPreFocusAwareEventTrigger = 0
        var childOnFocusAwareEventTrigger = 0
        var childOnPreFocusAwareEventTrigger = 0
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onFocusAwareEvent {
                        parentOnFocusAwareEventTrigger = triggerIndex++
                        false
                    }
                    .onPreFocusAwareEvent {
                        parentOnPreFocusAwareEventTrigger = triggerIndex++
                        false
                    }
            ) {
                Box(
                    modifier = Modifier
                        .onFocusAwareEvent {
                            childOnFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .onPreFocusAwareEvent {
                            childOnPreFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .focusable(initiallyFocused = true)
                )
            }
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(parentOnPreFocusAwareEventTrigger).isEqualTo(1)
            assertThat(childOnPreFocusAwareEventTrigger).isEqualTo(2)
            assertThat(childOnFocusAwareEventTrigger).isEqualTo(3)
            assertThat(parentOnFocusAwareEventTrigger).isEqualTo(4)
        }
    }

    @Test
    fun grandParent_parent_child() {
        // Arrange.
        var triggerIndex = 1
        var grandParentOnFocusAwareEventTrigger = 0
        var grandParentOnPreFocusAwareEventTrigger = 0
        var parentOnFocusAwareEventTrigger = 0
        var parentOnPreFocusAwareEventTrigger = 0
        var childOnFocusAwareEventTrigger = 0
        var childOnPreFocusAwareEventTrigger = 0
        ContentWithInitialFocus {
            Box(
                modifier = Modifier
                    .onFocusAwareEvent {
                        grandParentOnFocusAwareEventTrigger = triggerIndex++
                        false
                    }
                    .onPreFocusAwareEvent {
                        grandParentOnPreFocusAwareEventTrigger = triggerIndex++
                        false
                    }
                    .focusable()
            ) {
                Box(
                    modifier = Modifier
                        .onFocusAwareEvent {
                            parentOnFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .onPreFocusAwareEvent {
                            parentOnPreFocusAwareEventTrigger = triggerIndex++
                            false
                        }
                        .focusable()
                ) {
                    Box(
                        modifier = Modifier
                            .onFocusAwareEvent {
                                childOnFocusAwareEventTrigger = triggerIndex++
                                false
                            }
                            .onPreFocusAwareEvent {
                                childOnPreFocusAwareEventTrigger = triggerIndex++
                                false
                            }
                            .focusable(initiallyFocused = true)
                    )
                }
            }
        }

        // Act.
        rule.onRoot().performFocusAwareInput(sentEvent)

        // Assert.
        rule.runOnIdle {
            assertThat(grandParentOnPreFocusAwareEventTrigger).isEqualTo(1)
            assertThat(parentOnPreFocusAwareEventTrigger).isEqualTo(2)
            assertThat(childOnPreFocusAwareEventTrigger).isEqualTo(3)
            assertThat(childOnFocusAwareEventTrigger).isEqualTo(4)
            assertThat(parentOnFocusAwareEventTrigger).isEqualTo(5)
            assertThat(grandParentOnFocusAwareEventTrigger).isEqualTo(6)
        }
    }

    private fun Modifier.focusable(initiallyFocused: Boolean = false) = this
        .then(if (initiallyFocused) Modifier.focusRequester(initialFocus) else Modifier)
        .focusTarget()

    private fun SemanticsNodeInteraction.performFocusAwareInput(sentEvent: FocusAwareTestEvent) {
        @OptIn(ExperimentalTestApi::class)
        performFocusAwareInput {
            rotateToScrollVertically(sentEvent.verticalScrollPixels)
        }
    }

    private fun ContentWithInitialFocus(content: @Composable () -> Unit) {
        rule.setContent {
            Box(modifier = Modifier.requiredSize(10.dp, 10.dp)) { content() }
        }
        rule.runOnIdle { initialFocus.requestFocus() }
    }
}
