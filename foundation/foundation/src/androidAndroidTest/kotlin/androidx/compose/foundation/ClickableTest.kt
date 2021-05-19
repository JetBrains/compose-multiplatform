/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.center
import androidx.compose.ui.test.centerLeft
import androidx.compose.ui.test.centerRight
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class ClickableTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun clickableTest_defaultSemantics() {
        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun clickableTest_disabledSemantics() {
        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(enabled = false) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun clickableTest_longClickSemantics() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(onLongClick = onClick) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performSemanticsAction(SemanticsActions.OnLongClick)

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun clickableTest_click() {
        var counter = 0
        val onClick: () -> Unit = {
            ++counter
        }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onClick = onClick)
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun clickableTest_clickOnChildBasicText() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box(modifier = Modifier.clickable(onClick = onClick)) {
                BasicText("Foo")
                BasicText("Bar")
            }
        }

        rule.onNodeWithText("Foo", substring = true).assertExists()
        rule.onNodeWithText("Bar", substring = true).assertExists()

        rule.onNodeWithText("Foo", substring = true).performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        rule.onNodeWithText("Bar", substring = true).performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    @LargeTest
    fun clickableTest_longClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(onLongClick = onClick) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun clickableTest_click_withLongClick() {
        var clickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                click()
            }

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun clickableTest_click_withDoubleClick() {
        var clickCounter = 0
        var doubleClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            onDoubleClick = onDoubleClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
        }
    }

    @Test
    @LargeTest
    fun clickableTest_click_withDoubleClick_andLongClick() {
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            onDoubleClick = onDoubleClick,
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.mainClock.advanceTimeUntil { doubleClickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        rule.mainClock.advanceTimeUntil { longClickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
        }
    }

    @Test
    fun clickableTest_doubleClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(onDoubleClick = onClick) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.mainClock.advanceTimeUntil { counter == 1 }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.mainClock.advanceTimeUntil { counter == 2 }
    }

    @Test
    fun clickableTest_interactionSource() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture { down(center) }

        val halfTapIndicationDelay = TapIndicationDelay / 2

        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        // Haven't reached the tap delay yet, so we shouldn't have started a press
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        // Advance past the tap delay
        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun clickableTest_interactionSource_immediateRelease() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                down(center)
                up()
            }

        // We haven't reached the tap delay, but we have finished a press so we should have
        // emitted both press and release
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun clickableTest_interactionSource_immediateCancel() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                down(center)
                cancel()
            }

        // We haven't reached the tap delay, and a cancel was emitted, so no press should ever be
        // shown
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
    }

    @Test
    fun clickableTest_interactionSource_immediateDrag() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .draggable(
                            state = rememberDraggableState {},
                            orientation = Orientation.Horizontal
                        )
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                down(centerLeft)
                moveTo(centerRight)
            }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        // We started a drag before the timeout, so no press should be emitted
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
    }

    @Test
    fun clickableTest_interactionSource_dragAfterTimeout() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .draggable(
                            state = rememberDraggableState {},
                            orientation = Orientation.Horizontal
                        )
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                down(centerLeft)
            }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                moveTo(centerRight)
            }

        // The drag should cancel the press
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun clickableTest_interactionSource_cancelledGesture() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture { down(center) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture { cancel() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun clickableTest_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitClickableText by mutableStateOf(true)

        var scope: CoroutineScope? = null

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitClickableText) {
                    BasicText(
                        "ClickableText",
                        modifier = Modifier
                            .testTag("myClickable")
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {}
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture { down(center) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Dispose clickable
        rule.runOnIdle {
            emitClickableText = false
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    @LargeTest
    fun clickableTest_click_withDoubleClick_andLongClick_disabled() {
        val enabled = mutableStateOf(false)
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            enabled = enabled.value,
                            onDoubleClick = onDoubleClick,
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
            enabled.value = true
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.mainClock.advanceTimeUntil { doubleClickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        rule.mainClock.advanceTimeUntil { longClickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
        }
    }

    @Test
    fun clickable_testInspectorValue_noIndicationOverload() {
        val onClick: () -> Unit = { }
        rule.setContent {
            val modifier = Modifier.combinedClickable(onClick = onClick) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("combinedClickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "enabled",
                "onClickLabel",
                "role",
                "onClick",
                "onDoubleClick",
                "onLongClick",
                "onLongClickLabel"
            )
        }
    }

    @Test
    fun clickable_testInspectorValue_fullParamsOverload() {
        val onClick: () -> Unit = { }
        rule.setContent {
            val modifier = Modifier.combinedClickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("combinedClickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "enabled",
                "onClickLabel",
                "onClick",
                "role",
                "onDoubleClick",
                "onLongClick",
                "onLongClickLabel",
                "indication",
                "interactionSource"
            )
        }
    }

    // integration test for b/184872415
    @Test
    fun tapGestureTest_tryAwaitRelease_ReturnsTrue() {
        val wasSuccess = mutableStateOf(false)
        rule.setContent {
            Box(
                Modifier
                    .size(100.dp)
                    .testTag("myClickable")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                wasSuccess.value = tryAwaitRelease()
                            }
                        )
                    }
            )
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        assertThat(wasSuccess.value).isTrue()
    }
}