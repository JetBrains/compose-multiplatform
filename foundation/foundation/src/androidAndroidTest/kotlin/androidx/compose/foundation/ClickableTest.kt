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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.center
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithSubstring
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.up
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
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
                        .clickable(onLongClick = onClick) {}
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

        rule.onNodeWithSubstring("Foo").assertExists()
        rule.onNodeWithSubstring("Bar").assertExists()

        rule.onNodeWithSubstring("Foo").performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        rule.onNodeWithSubstring("Bar").performClick()

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
                        .clickable(onLongClick = onClick) {}
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
                        .clickable(
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
        val clickLatch = CountDownLatch(1)
        var doubleClickCounter = 0
        val onClick: () -> Unit = { clickLatch.countDown() }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            onDoubleClick = onDoubleClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        val res = clickLatch.await(1000, TimeUnit.MILLISECONDS)
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(res).isTrue()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @Test
    @LargeTest
    fun clickableTest_click_withDoubleClick_andLongClick() {
        val clickLatch = CountDownLatch(1)
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { clickLatch.countDown() }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            onDoubleClick = onDoubleClick,
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        val res = clickLatch.await(1000, TimeUnit.MILLISECONDS)
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(res).isTrue()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
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
                        .clickable(onDoubleClick = onClick) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun clickableTest_interactionState() {
        val interactionState = InteractionState()

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(interactionState = interactionState) {}
                )
            }
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture { down(center) }

        rule.runOnIdle {
            assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture { up() }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }

    @Test
    fun clickableTest_interactionState_resetWhenDisposed() {
        val interactionState = InteractionState()
        var emitClickableText by mutableStateOf(true)

        rule.setContent {
            Box {
                if (emitClickableText) {
                    BasicText(
                        "ClickableText",
                        modifier = Modifier
                            .testTag("myClickable")
                            .clickable(interactionState = interactionState) {}
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture { down(center) }

        rule.runOnIdle {
            assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        // Dispose clickable
        rule.runOnIdle {
            emitClickableText = false
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }

    @Test
    @LargeTest
    fun clickableTest_click_withDoubleClick_andLongClick_disabled() {
        val enabled = mutableStateOf(false)
        val clickLatch = CountDownLatch(1)
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { clickLatch.countDown() }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
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

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.count).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.count).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.count).isEqualTo(1)
            enabled.value = true
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        val res = clickLatch.await(1000, TimeUnit.MILLISECONDS)
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(res).isTrue()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }

        rule.onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @Test
    fun testInspectorValue() {
        val onClick: () -> Unit = { }
        rule.setContent {
            val modifier = Modifier.clickable(onClick = onClick) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("clickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "enabled",
                "onClickLabel",
                "onClick",
                "onDoubleClick",
                "onLongClick",
                "onLongClickLabel",
                "indication",
                "interactionState"
            )
        }
    }
}