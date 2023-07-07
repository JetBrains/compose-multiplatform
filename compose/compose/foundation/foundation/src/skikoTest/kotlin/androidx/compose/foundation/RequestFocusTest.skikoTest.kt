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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import kotlin.test.Test

class RequestFocusSkikoTest {

    @Test
    fun clickable_should_request_focus_on_click() =
        ImageComposeScene(
            width = 100,
            height = 100,
            density = Density(1f)
        ).use { scene ->
            var focusState: FocusState? = null
            var clicked = false
            scene.setContent {
                Box(modifier = Modifier
                    .size(25.dp)
                    .onFocusChanged { focusState = it }
                    .clickable { clicked = true }
                )
            }

            assertThat(clicked).isEqualTo(false)
            assertThat(focusState?.isFocused).isEqualTo(false)

            val downButtons = PointerButtons(isPrimaryPressed = true)
            val upButtons = PointerButtons(isPrimaryPressed = false)
            scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f), buttons = downButtons)
            scene.sendPointerEvent(PointerEventType.Release, Offset(10f, 10f), buttons = upButtons)

            assertThat(clicked).isEqualTo(true)
            assertThat(focusState?.hasFocus).isEqualTo(true)
        }


    @Test
    fun toggleable_should_request_focus_on_click() =
        ImageComposeScene(
            width = 100,
            height = 100,
            density = Density(1f)
        ).use { scene ->
            var focusState: FocusState? = null
            var clicked = false
            scene.setContent {
                Box(modifier = Modifier
                    .size(25.dp)
                    .onFocusChanged { focusState = it }
                    .toggleable(false) { clicked = true }
                )
            }

            assertThat(clicked).isEqualTo(false)
            assertThat(focusState?.isFocused).isEqualTo(false)

            val downButtons = PointerButtons(isPrimaryPressed = true)
            val upButtons = PointerButtons(isPrimaryPressed = false)
            scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f), buttons = downButtons)
            scene.sendPointerEvent(PointerEventType.Release, Offset(10f, 10f), buttons = upButtons)

            assertThat(clicked).isEqualTo(true)
            assertThat(focusState?.hasFocus).isEqualTo(true)
        }
}
