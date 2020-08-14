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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.filters.MediumTest
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Stack
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsNotEnabled
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.performClick
import androidx.ui.test.performGesture
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onNodeWithSubstring
import androidx.ui.test.runOnIdle
import androidx.ui.test.click
import androidx.ui.test.doubleClick
import androidx.ui.test.down
import androidx.ui.test.longClick
import androidx.ui.test.up
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class ClickableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickableTest_defaultSemantics() {
        composeTestRule.setContent {
            Stack {
                Text("ClickableText", modifier = Modifier.testTag("myClickable").clickable {})
            }
        }

        onNodeWithTag("myClickable")
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun clickableTest_disabledSemantics() {
        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(enabled = false) {}
                )
            }
        }

        onNodeWithTag("myClickable")
            .assertIsNotEnabled()
            .assertHasNoClickAction()
    }

    @Test
    fun clickableTest_click() {
        var counter = 0
        val onClick: () -> Unit = {
            ++counter
        }

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onClick = onClick)
                )
            }
        }

        onNodeWithTag("myClickable")
            .performClick()

        runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        onNodeWithTag("myClickable")
            .performClick()

        runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun clickableTest_clickOnChildText() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        composeTestRule.setContent {
            Stack(modifier = Modifier.clickable(onClick = onClick)) {
                Text("Foo")
                Text("Bar")
            }
        }

        onNodeWithSubstring("Foo").assertExists()
        onNodeWithSubstring("Bar").assertExists()

        onNodeWithSubstring("Foo").performClick()

        runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        onNodeWithSubstring("Bar").performClick()

        runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun clickableTest_longClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onLongClick = onClick) {}
                )
            }
        }

        onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun clickableTest_click_withLongClick() {
        var clickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        composeTestRule.setContent {
            Stack {
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

        onNodeWithTag("myClickable")
            .performGesture {
                click()
            }

        runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
        }

        onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        runOnIdle {
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

        composeTestRule.setContent {
            Stack {
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

        onNodeWithTag("myClickable")
            .performClick()

        val res = clickLatch.await(1000, TimeUnit.MILLISECONDS)
        runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(res).isTrue()
        }

        onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        runOnIdle {
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

        composeTestRule.setContent {
            Stack {
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

        onNodeWithTag("myClickable")
            .performClick()

        val res = clickLatch.await(1000, TimeUnit.MILLISECONDS)
        runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(res).isTrue()
        }

        onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }

        onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @Test
    fun clickableTest_doubleClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onDoubleClick = onClick) {}
                )
            }
        }

        onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun clickableTest_interactionState() {
        val interactionState = InteractionState()

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(interactionState = interactionState) {}
                )
            }
        }

        runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        onNodeWithTag("myClickable")
            .performGesture { down(center) }

        runOnIdle {
            assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        onNodeWithTag("myClickable")
            .performGesture { up() }

        runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }

    @Test
    fun clickableTest_interactionState_resetWhenDisposed() {
        val interactionState = InteractionState()
        var emitClickableText by mutableStateOf(true)

        composeTestRule.setContent {
            Stack {
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

        runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        onNodeWithTag("myClickable")
            .performGesture { down(center) }

        runOnIdle {
            assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        // Dispose clickable
        runOnIdle {
            emitClickableText = false
        }

        runOnIdle {
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

        composeTestRule.setContent {
            Stack {
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

        onNodeWithTag("myClickable")
            .performClick()

        runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.count).isEqualTo(1)
        }

        onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.count).isEqualTo(1)
        }

        onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.count).isEqualTo(1)
            enabled.value = true
        }

        onNodeWithTag("myClickable")
            .performClick()

        val res = clickLatch.await(1000, TimeUnit.MILLISECONDS)
        runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(res).isTrue()
        }

        onNodeWithTag("myClickable")
            .performGesture {
                doubleClick()
            }

        runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }

        onNodeWithTag("myClickable")
            .performGesture {
                longClick()
            }

        runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }
}