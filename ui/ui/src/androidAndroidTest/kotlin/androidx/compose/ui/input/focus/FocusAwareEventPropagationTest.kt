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

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.setFocusableContent
import androidx.compose.ui.input.focus.FocusAwareEventPropagationTest.NodeType.KeyInput
import androidx.compose.ui.input.focus.FocusAwareEventPropagationTest.NodeType.RotaryInput
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputInputModifierNodeImpl
import androidx.compose.ui.input.rotary.RotaryInputModifierNodeImpl
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Focus-aware event propagation test.
 *
 * This test verifies the event propagation logic using
 * [androidx.compose.ui.input.rotary.RotaryScrollEvent]s, but it is meant to test the generic
 * propagation logic for all focus-aware events.
 */
@MediumTest
@RunWith(Parameterized::class)
class FocusAwareEventPropagationTest(private val nodeType: NodeType) {
    @get:Rule
    val rule = createComposeRule()

    private val sentEvent: Any = when (nodeType) {
        KeyInput ->
            KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_A))
        RotaryInput ->
            RotaryScrollEvent(1f, 1f, 0L)
    }
    private var receivedEvent: Any? = null
    private val initialFocus = FocusRequester()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "node = {0}")
        fun initParameters() = arrayOf(KeyInput, RotaryInput)
    }

    @Test
    fun noFocusable_doesNotDeliverEvent() {
        // Arrange.
        var error: IllegalStateException? = null
        rule.setContent {
            Box(
                modifier = Modifier.onFocusAwareEvent {
                    receivedEvent = it
                    true
                }
            )
        }

        // Act.
        try {
            rule.onRoot().performFocusAwareInput(sentEvent)
        } catch (exception: IllegalStateException) {
            error = exception
        }

        // Assert.
        assertThat(receivedEvent).isNull()
        when (nodeType) {
            KeyInput -> assertThat(error!!.message).contains("do not have an active focus target")
            RotaryInput -> assertThat(error).isNull()
        }
    }

    @Test
    fun unfocusedFocusable_doesNotDeliverEvent() {
        // Arrange.
        var error: IllegalStateException? = null
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
        try {
            rule.onRoot().performFocusAwareInput(sentEvent)
        } catch (exception: IllegalStateException) {
            error = exception
        }

        // Assert.
        assertThat(receivedEvent).isNull()
        when (nodeType) {
            KeyInput -> assertThat(error!!.message).contains("do not have an active focus target")
            RotaryInput -> assertThat(error).isNull()
        }
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
        when (nodeType) {
            KeyInput -> assertThat(receivedEvent).isEqualTo(sentEvent)
            RotaryInput -> assertThat(receivedEvent).isNull()
        }
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
        when (nodeType) {
            KeyInput -> assertThat(receivedEvent).isEqualTo(sentEvent)
            RotaryInput -> assertThat(receivedEvent).isNull()
        }
    }

    @Ignore("b/265319988")
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
        rule.runOnIdle { assertThat(sentEvent).isEqualTo(receivedEvent) }
    }

    @Ignore("b/264466323")
    @Test
    fun onPreFocusAwareEvent_triggered() {
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
        rule.runOnIdle { assertThat(sentEvent).isEqualTo(receivedEvent) }
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

    @Ignore // b/266984867
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

    private fun SemanticsNodeInteraction.performFocusAwareInput(sentEvent: Any) {
        when (nodeType) {
            KeyInput -> {
                check(sentEvent is KeyEvent)
                performKeyPress(sentEvent)
            }
            RotaryInput -> {
                check(sentEvent is RotaryScrollEvent)
                @OptIn(ExperimentalTestApi::class)
                performRotaryScrollInput {
                    rotateToScrollVertically(sentEvent.verticalScrollPixels)
                }
            }
        }
    }

    private fun ContentWithInitialFocus(content: @Composable () -> Unit) {
        rule.setContent {
            Box(modifier = Modifier.requiredSize(10.dp, 10.dp)) { content() }
        }
        rule.runOnIdle { initialFocus.requestFocus() }
    }

    private fun Modifier.onFocusAwareEvent(onEvent: (Any) -> Boolean): Modifier = this.then(
        FocusAwareEventElement(onEvent, nodeType, EventType.OnEvent)
    )

    private fun Modifier.onPreFocusAwareEvent(onEvent: (Any) -> Boolean): Modifier = this.then(
        FocusAwareEventElement(onEvent, nodeType, EventType.PreEvent)
    )

    @OptIn(ExperimentalComposeUiApi::class)
    private data class FocusAwareEventElement(
        private val callback: (Any) -> Boolean,
        private val nodeType: NodeType,
        private val eventType: EventType
    ) : ModifierNodeElement<Modifier.Node>() {
        override fun create() = when (nodeType) {
            KeyInput -> KeyInputInputModifierNodeImpl(
                onEvent = callback.takeIf { eventType == EventType.OnEvent },
                onPreEvent = callback.takeIf { eventType == EventType.PreEvent }
            )
            RotaryInput -> RotaryInputModifierNodeImpl(
                onEvent = callback.takeIf { eventType == EventType.OnEvent },
                onPreEvent = callback.takeIf { eventType == EventType.PreEvent }
            )
        }

        override fun update(node: Modifier.Node) = when (nodeType) {
            KeyInput -> (node as KeyInputInputModifierNodeImpl).apply {
                onEvent = callback.takeIf { eventType == EventType.OnEvent }
                onPreEvent = callback.takeIf { eventType == EventType.PreEvent }
            }
            RotaryInput -> (node as RotaryInputModifierNodeImpl).apply {
                onEvent = callback.takeIf { eventType == EventType.OnEvent }
                onPreEvent = callback.takeIf { eventType == EventType.PreEvent }
            }
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "onEvent"
            properties["onEvent"] = callback
        }
    }

    enum class NodeType { KeyInput, RotaryInput }
    enum class EventType { PreEvent, OnEvent }
}
