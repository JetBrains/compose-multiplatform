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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.In
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.node.Owner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN as KeyDown
import android.view.KeyEvent.META_SHIFT_ON as Shift

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class KeyEventToFocusDirectionTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var owner: Owner

    @Before
    fun setup() {
        rule.setContent {
            owner = LocalView.current as Owner
        }
    }

    @Test
    fun left() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(KeyDown, Key.DirectionLeft.nativeKeyCode))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        assertThat(focusDirection).isEqualTo(Left)
    }

    @Test
    fun right() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(KeyDown, Key.DirectionRight.nativeKeyCode))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        assertThat(focusDirection).isEqualTo(Right)
    }

    @Test
    fun up() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(KeyDown, Key.DirectionUp.nativeKeyCode))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        assertThat(focusDirection).isEqualTo(Up)
    }

    @Test
    fun down() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(KeyDown, Key.DirectionDown.nativeKeyCode))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        assertThat(focusDirection).isEqualTo(Down)
    }

    @Test
    fun tab_next() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(KeyDown, Key.Tab.nativeKeyCode))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        assertThat(focusDirection).isEqualTo(Next)
    }

    @Test
    fun shiftTab_previous() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(0L, 0L, KeyDown, Key.Tab.nativeKeyCode, 0, Shift))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        assertThat(focusDirection).isEqualTo(Previous)
    }

    @Test
    fun dpadCenter_in() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(KeyDown, Key.DirectionCenter.nativeKeyCode))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        @OptIn(ExperimentalComposeUiApi::class)
        assertThat(focusDirection).isEqualTo(In)
    }

    @Test
    fun enter_in() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(KeyDown, Key.Enter.nativeKeyCode))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        @OptIn(ExperimentalComposeUiApi::class)
        assertThat(focusDirection).isEqualTo(In)
    }

    @Test
    fun numpadEnter_in() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(KeyDown, Key.NumPadEnter.nativeKeyCode))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        @OptIn(ExperimentalComposeUiApi::class)
        assertThat(focusDirection).isEqualTo(In)
    }

    @Test
    fun back_out() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(KeyDown, Key.Back.nativeKeyCode))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        @OptIn(ExperimentalComposeUiApi::class)
        assertThat(focusDirection).isEqualTo(Out)
    }

    @Test
    fun esc_out() {
        // Arrange.
        val keyEvent = KeyEvent(AndroidKeyEvent(KeyDown, Key.Escape.nativeKeyCode))

        // Act.
        val focusDirection = owner.getFocusDirection(keyEvent)

        // Assert.
        @OptIn(ExperimentalComposeUiApi::class)
        assertThat(focusDirection).isEqualTo(Out)
    }
}
