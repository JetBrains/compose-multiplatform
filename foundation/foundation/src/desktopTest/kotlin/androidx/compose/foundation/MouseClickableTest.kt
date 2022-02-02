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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
class MouseClickableTest {
    @Test
    fun click() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        val clicks = mutableListOf<MouseClickScope>()

        scene.setContent {
            Box(
                modifier = Modifier
                    .mouseClickable {
                        clicks.add(MouseClickScope(buttons, keyboardModifiers))
                    }
                    .size(10.dp, 20.dp)
            )
        }

        var downButtons = PointerButtons(isSecondaryPressed = true)
        var upButtons = PointerButtons(isSecondaryPressed = false)
        val downKeyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = true)
        val upKeyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = true, isShiftPressed = true)
        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(
            PointerEventType.Press, Offset(0f, 0f), buttons = downButtons, keyboardModifiers = downKeyboardModifiers
        )
        scene.sendPointerEvent(
            PointerEventType.Release, Offset(0f, 0f), buttons = upButtons, keyboardModifiers = upKeyboardModifiers
        )
        assertThat(clicks.size).isEqualTo(1)
        assertThat(clicks.last().buttons).isEqualTo(downButtons)
        assertThat(clicks.last().keyboardModifiers).isEqualTo(downKeyboardModifiers)

        downButtons = PointerButtons(isPrimaryPressed = true)
        upButtons = PointerButtons(isPrimaryPressed = false)
        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        scene.sendPointerEvent(
            PointerEventType.Press, Offset(5f, 5f), buttons = downButtons, keyboardModifiers = downKeyboardModifiers
        )
        scene.sendPointerEvent(
            PointerEventType.Release, Offset(5f, 5f), buttons = upButtons, keyboardModifiers = upKeyboardModifiers
        )
        assertThat(clicks.size).isEqualTo(2)
        assertThat(clicks.last().buttons).isEqualTo(downButtons)
        assertThat(clicks.last().keyboardModifiers).isEqualTo(downKeyboardModifiers)
    }

    @Test
    fun `consume click`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var outerBoxClicks = 0
        var innerBoxClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .clickable {
                        outerBoxClicks++
                    }
                    .size(40.dp, 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .mouseClickable {
                            innerBoxClicks++
                        }
                        .size(10.dp, 20.dp)
                )
            }
        }

        val downButtons = PointerButtons(isPrimaryPressed = true)
        val upButtons = PointerButtons(isPrimaryPressed = false)
        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), buttons = upButtons)
        assertThat(outerBoxClicks).isEqualTo(0)
        assertThat(innerBoxClicks).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 30f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(30f, 30f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(30f, 30f), buttons = upButtons)
        assertThat(outerBoxClicks).isEqualTo(1)
        assertThat(innerBoxClicks).isEqualTo(1)
    }

    @Test
    fun `don't handle consumed click by another click`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var outerBoxClicks = 0
        var innerBoxClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .mouseClickable {
                        outerBoxClicks++
                    }
                    .size(40.dp, 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clickable {
                            innerBoxClicks++
                        }
                        .size(10.dp, 20.dp)
                )
            }
        }

        val downButtons = PointerButtons(isPrimaryPressed = true)
        val upButtons = PointerButtons(isPrimaryPressed = false)
        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), buttons = upButtons)
        assertThat(outerBoxClicks).isEqualTo(0)
        assertThat(innerBoxClicks).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 30f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(30f, 30f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(30f, 30f), buttons = upButtons)
        assertThat(outerBoxClicks).isEqualTo(1)
        assertThat(innerBoxClicks).isEqualTo(1)
    }

    @Test
    fun `don't handle consumed click by pan`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var outerBoxTotalPan = Offset.Zero
        var innerBoxClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, _, _ ->
                            outerBoxTotalPan += pan
                        }
                    }
                    .size(80.dp, 80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .mouseClickable {
                            innerBoxClicks++
                        }
                        .size(50.dp, 50.dp)
                )
            }
        }

        val downButtons = PointerButtons(isPrimaryPressed = true)
        val upButtons = PointerButtons(isPrimaryPressed = false)
        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Move, Offset(20f, 0f))
        scene.sendPointerEvent(PointerEventType.Release, Offset(20f, 0f), buttons = upButtons)
        assertThat(outerBoxTotalPan).isEqualTo(Offset(20f, 0f))
        assertThat(innerBoxClicks).isEqualTo(0)

        scene.sendPointerEvent(PointerEventType.Press, Offset(20f, 0f), buttons = downButtons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(20f, 0f), buttons = upButtons)
        assertThat(outerBoxTotalPan).isEqualTo(Offset(20f, 0f))
        assertThat(innerBoxClicks).isEqualTo(1)
    }
}