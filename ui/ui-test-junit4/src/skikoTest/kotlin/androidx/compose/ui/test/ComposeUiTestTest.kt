/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Scroll
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.PointerType.Companion.Mouse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class ComposeUiTestTest {
    private val events = mutableListOf<PointerEvent>()

    @Composable
    private fun TestEventBox() {
        Box(
            Modifier
                .size(100f.dp, 100f.dp)
                .testTag("test")
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            events += awaitPointerEvent()
                        }
                    }
                }
        )
    }

    @Test
    fun mouse_move() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performMouseInput {
            moveTo(Offset(30f, 40f))
            moveTo(Offset(30f, 50f))
        }
        assertThat(events).hasSize(3)
        events[0].apply {
            assertThat(type).isEqualTo(Enter)
            assertThat(button).isEqualTo(null)
            assertThat(buttons).isEqualTo(PointerButtons())
            assertThat(changes[0].position).isEqualTo(Offset(30f, 40f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[1].apply {
            assertThat(type).isEqualTo(Move)
            assertThat(button).isEqualTo(null)
            assertThat(buttons).isEqualTo(PointerButtons())
            assertThat(changes[0].position).isEqualTo(Offset(30f, 40f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[2].apply {
            assertThat(type).isEqualTo(Move)
            assertThat(button).isEqualTo(null)
            assertThat(buttons).isEqualTo(PointerButtons())
            assertThat(changes[0].position).isEqualTo(Offset(30f, 50f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
    }

    @Test
    fun mouse_drag() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performMouseInput {
            moveTo(Offset(30f, 40f))
            press()
            moveTo(Offset(10f, 20f))
        }
        assertThat(events).hasSize(4)
        events[0].apply {
            assertThat(type).isEqualTo(Enter)
            assertThat(button).isEqualTo(null)
            assertThat(buttons).isEqualTo(PointerButtons())
            assertThat(changes[0].position).isEqualTo(Offset(30f, 40f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[1].apply {
            assertThat(type).isEqualTo(Move)
            assertThat(button).isEqualTo(null)
            assertThat(buttons).isEqualTo(PointerButtons())
            assertThat(changes[0].position).isEqualTo(Offset(30f, 40f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[2].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(button).isEqualTo(PointerButton.Primary)
            assertThat(buttons).isEqualTo(PointerButtons(isPrimaryPressed = true))
            assertThat(changes[0].position).isEqualTo(Offset(30f, 40f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[3].apply {
            assertThat(type).isEqualTo(Move)
            assertThat(button).isEqualTo(null)
            assertThat(buttons).isEqualTo(PointerButtons(isPrimaryPressed = true))
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
    }

    @Test
    fun mouse_press_primary() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performMouseInput {
            press()
        }
        assertThat(events).hasSize(1)
        events[0].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(button).isEqualTo(PointerButton.Primary)
            assertThat(buttons).isEqualTo(PointerButtons(isPrimaryPressed = true))
            assertThat(changes[0].position).isEqualTo(Offset.Zero)
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
    }

    @Test
    fun mouse_press_and_release_primary() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performMouseInput {
            press()
            release()
        }
        assertThat(events).hasSize(2)
        events[0].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(button).isEqualTo(PointerButton.Primary)
            assertThat(buttons).isEqualTo(PointerButtons(isPrimaryPressed = true))
            assertThat(changes[0].position).isEqualTo(Offset.Zero)
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[1].apply {
            assertThat(type).isEqualTo(Release)
            assertThat(button).isEqualTo(PointerButton.Primary)
            assertThat(buttons).isEqualTo(PointerButtons(isPrimaryPressed = false))
            assertThat(changes[0].position).isEqualTo(Offset.Zero)
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
    }

    @Test
    fun mouse_click() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performMouseInput {
            click(Offset(10f, 20f))
        }
        assertThat(events).hasSize(2)
        events[0].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(button).isEqualTo(PointerButton.Primary)
            assertThat(buttons).isEqualTo(PointerButtons(isPrimaryPressed = true))
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[1].apply {
            assertThat(type).isEqualTo(Release)
            assertThat(button).isEqualTo(PointerButton.Primary)
            assertThat(buttons).isEqualTo(PointerButtons(isPrimaryPressed = false))
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
    }

    @Test
    fun mouse_press_multiple() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performMouseInput {
            updatePointerTo(Offset(10f, 20f))
            press(MouseButton.Primary)
            press(MouseButton.Tertiary)
            release(MouseButton.Primary)
            release(MouseButton.Tertiary)
            press(MouseButton(3))
        }
        assertThat(events).hasSize(5)
        events[0].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(button).isEqualTo(PointerButton.Primary)
            assertThat(buttons).isEqualTo(PointerButtons(isPrimaryPressed = true))
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[1].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(button).isEqualTo(PointerButton.Tertiary)
            assertThat(buttons).isEqualTo(PointerButtons(isPrimaryPressed = true, isTertiaryPressed = true))
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[2].apply {
            assertThat(type).isEqualTo(Release)
            assertThat(button).isEqualTo(PointerButton.Primary)
            assertThat(buttons).isEqualTo(PointerButtons(isTertiaryPressed = true))
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[3].apply {
            assertThat(type).isEqualTo(Release)
            assertThat(button).isEqualTo(PointerButton.Tertiary)
            assertThat(buttons).isEqualTo(PointerButtons())
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[4].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(button).isEqualTo(PointerButton.Back)
            assertThat(buttons).isEqualTo(PointerButtons(isBackPressed = true))
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
    }

    @Test
    fun mouse_scroll() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performMouseInput {
            updatePointerTo(Offset(30f, 40f))
            scroll(20f)
            scroll(-20f, ScrollWheel.Horizontal)
        }
        assertThat(events).hasSize(2)
        events[0].apply {
            assertThat(type).isEqualTo(Scroll)
            assertThat(button).isEqualTo(null)
            assertThat(buttons).isEqualTo(PointerButtons())
            assertThat(changes[0].scrollDelta).isEqualTo(Offset(0f, 20f))
            assertThat(changes[0].position).isEqualTo(Offset(30f, 40f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
        events[1].apply {
            assertThat(type).isEqualTo(Scroll)
            assertThat(button).isEqualTo(null)
            assertThat(buttons).isEqualTo(PointerButtons())
            assertThat(changes[0].scrollDelta).isEqualTo(Offset(-20f, 0f))
            assertThat(changes[0].position).isEqualTo(Offset(30f, 40f))
            assertThat(changes[0].type).isEqualTo(Mouse)
        }
    }

    @Test
    fun touch_drag() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performTouchInput {
            down(Offset(30f, 40f))
            moveTo(Offset(10f, 20f))
            moveTo(Offset(30f, 50f))
        }
        assertThat(events).hasSize(3)
        events[0].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(changes[0].position).isEqualTo(Offset(30f, 40f))
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
        events[1].apply {
            assertThat(type).isEqualTo(Enter)
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
        events[2].apply {
            assertThat(type).isEqualTo(Move)
            assertThat(changes[0].position).isEqualTo(Offset(30f, 50f))
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
    }

    @Test
    fun touch_press() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performTouchInput {
            down(Offset.Zero)
        }
        assertThat(events).hasSize(1)
        events[0].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(changes[0].position).isEqualTo(Offset.Zero)
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
    }

    @Test
    fun touch_press_and_release() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performTouchInput {
            down(Offset.Zero)
            up()
        }
        assertThat(events).hasSize(2)
        events[0].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(changes[0].position).isEqualTo(Offset.Zero)
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
        events[1].apply {
            assertThat(type).isEqualTo(Release)
            assertThat(changes[0].position).isEqualTo(Offset.Zero)
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
    }

    @Test
    fun touch_click() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performTouchInput {
            click(Offset(10f, 20f))
        }
        assertThat(events).hasSize(3)
        events[0].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
        events[1].apply {
            assertThat(type).isEqualTo(Enter)
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
        events[2].apply {
            assertThat(type).isEqualTo(Release)
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
    }

    @Test
    fun touch_press_multiple() = runComposeUiTest {
        setContent { TestEventBox() }

        onNodeWithTag("test").performTouchInput {
            down(0, Offset(0f, 0f))
            down(1, Offset(10f, 20f))
            up(0)
            up(1)
        }

        val events = events.filter { it.type != Move && it.type != Enter && it.type != Exit }

        assertThat(events).hasSize(4)
        events[0].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(changes[0].position).isEqualTo(Offset(0f, 0f))
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
        events[1].apply {
            assertThat(type).isEqualTo(Press)
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
        events[2].apply {
            assertThat(type).isEqualTo(Release)
            assertThat(changes[0].position).isEqualTo(Offset(0f, 0f))
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
        events[3].apply {
            assertThat(type).isEqualTo(Release)
            assertThat(changes[0].position).isEqualTo(Offset(10f, 20f))
            assertThat(changes[0].type).isEqualTo(PointerType.Touch)
        }
    }

    @Test
    fun text_input() = runComposeUiTest {
        var text by mutableStateOf(TextFieldValue(""))
        val focusRequester = FocusRequester()

        setContent {
            BasicTextField(text, { text = it }, modifier = Modifier.testTag("test").focusRequester(focusRequester))
        }

        onNodeWithTag("test").performClick()

        onNodeWithTag("test").performTextInput("Text")
        assertThat(text.text).isEqualTo("Text")

        onNodeWithTag("test").performTextInput("Text")
        assertThat(text.text).isEqualTo("TextText")

        onNodeWithTag("test").performTextInputSelection(TextRange(1, 2))
        assertThat(text.selection).isEqualTo(TextRange(1, 2))

        onNodeWithTag("test").performTextClearance()
        assertThat(text.text).isEqualTo("")
    }
}

private class AssertThat<T>(val t: T)

private fun <T> AssertThat<T>.isEqualTo(a: Any?) {
    assertEquals(a, t)
}

private fun <T : Collection<*>> AssertThat<T>.hasSize(size: Int) {
    assertEquals(size, t.size, "Expected size = $size, but was ${t.size}")
}
private fun <T> assertThat(t: T): AssertThat<T> {
    return AssertThat(t)
}
