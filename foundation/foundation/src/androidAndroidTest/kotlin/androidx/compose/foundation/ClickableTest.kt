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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.filters.MediumTest
import androidx.ui.test.SemanticsMatcher
import androidx.ui.test.assert
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsNotEnabled
import androidx.ui.test.center
import androidx.ui.test.click
import androidx.ui.test.createComposeRule
import androidx.ui.test.doubleClick
import androidx.ui.test.down
import androidx.ui.test.longClick
import androidx.ui.test.onNodeWithSubstring
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performClick
import androidx.ui.test.performGesture
import androidx.ui.test.performSemanticsAction
import androidx.ui.test.up
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class ClickableTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun clickableTest_defaultSemantics() {
        rule.setContent {
            Box {
                Text("ClickableText", modifier = Modifier.testTag("myClickable").clickable {})
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
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(enabled = false) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .assertIsNotEnabled()
            .assertHasNoClickAction()
    }

    @Test
    fun clickableTest_longClickSemantics() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onLongClick = onClick) {}
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
                Text(
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
    fun clickableTest_clickOnChildText() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box(modifier = Modifier.clickable(onClick = onClick)) {
                Text("Foo")
                Text("Bar")
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
    fun clickableTest_longClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onLongClick = onClick) {}
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
                Text(
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
                Text(
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
    fun clickableTest_click_withDoubleClick_andLongClick() {
        val clickLatch = CountDownLatch(1)
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { clickLatch.countDown() }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                Text(
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
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onDoubleClick = onClick) {}
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
                Text(
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
                    Text(
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
                Text(
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
}