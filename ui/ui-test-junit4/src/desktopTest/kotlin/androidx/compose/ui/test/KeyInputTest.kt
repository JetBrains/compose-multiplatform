/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test


/**
 * Tests the key-event sending functionality of the test framework.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class KeyInputTest {

    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun FocusedBox(
        onKeyEvent: ((KeyEvent) -> Unit)? = null
    ){
        val focusRequester = remember { FocusRequester() }
        Box(
            Modifier
                .testTag("tag")
                .size(100.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent{
                    onKeyEvent?.invoke(it)
                    true
                }
        )

        LaunchedEffect(Unit){
            focusRequester.requestFocus()
        }
    }

    @Test
    fun testKeyDownAndUp() {
        var keyEvent: KeyEvent? = null

        rule.setContent {
            FocusedBox {
                keyEvent = it
            }
        }

        with(rule.onNodeWithTag("tag")){
            performKeyInput {
                keyDown(Key.C)
            }
            assertNotNull(keyEvent, "Key event not detected")
            keyEvent!!.let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key event type")
            }
            keyEvent = null

            performKeyInput {
                keyUp(Key.C)
            }
            assertNotNull(keyEvent, "Key event not detected")
            keyEvent!!.let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyUp, it.type, "Wrong key event type")
            }
        }
    }

    @Test
    fun testIsKeyDown() {
        rule.setContent {
            FocusedBox()
        }

        with(rule.onNodeWithTag("tag")){
            performKeyInput {
                keyDown(Key.C)
                assertTrue(isKeyDown(Key.C), "Key is not down")
                assertFalse(isKeyDown(Key.A), "Unexpected key is down")
                keyUp(Key.C)
                assertFalse(isKeyDown(Key.C), "Key is down")
            }
        }
    }

    @Test
    fun testPressKey() {
        val keyEvents = mutableListOf<KeyEvent>()

        rule.setContent {
            FocusedBox {
                keyEvents.add(it)
            }
        }

        with(rule.onNodeWithTag("tag")){
            performKeyInput {
                pressKey(Key.C)
            }
            assertEquals(2, keyEvents.size)
            keyEvents[0].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key type")
            }
            keyEvents[1].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyUp, it.type, "Wrong key type")
            }
        }
    }

    @Test
    fun testWithKeyDown() {
        val keyEvents = mutableListOf<KeyEvent>()

        rule.setContent {
            FocusedBox {
                keyEvents.add(it)
            }
        }

        with(rule.onNodeWithTag("tag")){
            performKeyInput {
                withKeyDown(Key.ShiftLeft){
                    pressKey(Key.C)
                }
            }
            assertEquals(4, keyEvents.size)
            keyEvents[0].let {
                assertEquals(Key.ShiftLeft, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key type")
            }
            keyEvents[1].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key type")
                assertTrue(it.isShiftPressed, "Shift is not pressed")
                assertEquals(it.utf16CodePoint, 0x43, "Wrong code point")  // Uppercase 'C'
            }
            keyEvents[2].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyUp, it.type, "Wrong key type")
            }
            keyEvents[3].let {
                assertEquals(Key.ShiftLeft, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyUp, it.type, "Wrong key type")
            }
        }
    }

    @Test
    fun testWithKeyToggled() {
        val keyEvents = mutableListOf<KeyEvent>()

        rule.setContent {
            FocusedBox {
                keyEvents.add(it)
            }
        }

        with(rule.onNodeWithTag("tag")){
            performKeyInput {
                withKeyToggled(Key.CapsLock){
                    assertTrue(isCapsLockOn)
                    pressKey(Key.C)
                }
            }
            assertEquals(6, keyEvents.size)
            keyEvents[2].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyDown, it.type, "Wrong key type")
                assertEquals(it.utf16CodePoint, 0x43, "Wrong code point")  // Uppercase 'C'
            }
            keyEvents[3].let {
                assertEquals(Key.C, it.key, "Wrong key")
                assertEquals(KeyEventType.KeyUp, it.type, "Wrong key type")
            }
        }
    }
}