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

package androidx.compose.ui.focus

import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN as KeyDown
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.Back
import androidx.compose.ui.input.key.Key.Companion.DirectionRight
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class ComposeViewKeyEventInteropTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun composeView_doesNotConsumesKeyEvent_ifTheContentIsNotFocusable() {
        // Arrange.
        rule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                BasicText("text")
            }
        }

        // Act.
        val keyEvent = AndroidKeyEvent(KeyDown, DirectionRight.nativeKeyCode)
        rule.activity.dispatchKeyEvent(keyEvent)

        // Assert.
        assertThat(rule.activity.receivedKeyEvent).isEqualTo(keyEvent)
    }

    @Test
    fun composeView_doesNotConsumesKeyEvent_ifFocusIsNotMoved() {
        // Arrange.
        val focusRequester = FocusRequester()
        rule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                BasicText(
                    text = "text",
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                )
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        val keyEvent = AndroidKeyEvent(KeyDown, DirectionRight.nativeKeyCode)
        rule.activity.dispatchKeyEvent(keyEvent)

        // Assert.
        assertThat(rule.activity.receivedKeyEvent).isEqualTo(keyEvent)
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun composeView_consumesKeyEvent_ifFocusIsMoved() {
        // Arrange.
        val (item1, item2) = FocusRequester.createRefs()
        rule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                Row {
                    BasicText(
                        text = "Item 1",
                        modifier = Modifier
                            .focusRequester(item1)
                            .focusProperties { right = item2 }
                            .focusable()
                    )
                    BasicText(
                        text = "Item 2",
                        modifier = Modifier
                            .focusRequester(item2)
                            .focusable()
                    )
                }
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        val keyEvent = AndroidKeyEvent(KeyDown, DirectionRight.nativeKeyCode)
        rule.activity.dispatchKeyEvent(keyEvent)

        // Assert.
        assertThat(rule.activity.receivedKeyEvent).isNull()
    }

    @Test
    fun composeView_doesNotConsumeBackKeyEvent_ifFocusMovesToRoot() {
        // Arrange.
        val (item1, item2) = FocusRequester.createRefs()
        rule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                BasicText(
                    text = "Item 1",
                    modifier = Modifier
                        .focusRequester(item1)
                        .focusProperties { down = item2 }
                        .focusable()
                )
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        val keyEvent = AndroidKeyEvent(KeyDown, Back.nativeKeyCode)
        rule.activity.dispatchKeyEvent(keyEvent)

        // Assert.
        assertThat(rule.activity.receivedKeyEvent).isEqualTo(keyEvent)
    }

    @Test
    fun composeView_consumesBackKeyEvent_ifFocusMovesToNonRoot() {
        // Arrange.
        val (item1, item2) = FocusRequester.createRefs()
        rule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                Box(Modifier.focusable()) {
                    BasicText(
                        text = "Item 1",
                        modifier = Modifier
                            .focusRequester(item1)
                            .focusProperties { down = item2 }
                            .focusable()
                    )
                }
            }
        }
        rule.runOnIdle { item1.requestFocus() }

        // Act.
        val keyEvent = AndroidKeyEvent(KeyDown, Back.nativeKeyCode)
        rule.activity.dispatchKeyEvent(keyEvent)

        // Assert.
        assertThat(rule.activity.receivedKeyEvent).isNull()
    }
}
