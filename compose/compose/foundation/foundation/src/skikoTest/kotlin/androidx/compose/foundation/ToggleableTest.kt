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
import androidx.compose.foundation.internal.keyEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class ToggleableTest {

    @Test
    fun toggleable_by_Space_button() = runComposeUiTest {
        var state: Boolean by mutableStateOf(false)
        setContent {
            Box(modifier = Modifier.size(100.dp, 100.dp).toggleable(
                value = state,
                onValueChange = {
                    state = it
                }
            ).testTag("toggle"))
        }

        runOnIdle {
            assertEquals(false, state)
        }

        onRoot().apply {
            performKeyPress(keyEvent(Key.Tab, KeyEventType.KeyDown))
            performKeyPress(keyEvent(Key.Tab, KeyEventType.KeyUp))
        }

        onNodeWithTag("toggle").apply {
            performKeyPress(keyEvent(Key.Spacebar, KeyEventType.KeyDown))
            performKeyPress(keyEvent(Key.Spacebar, KeyEventType.KeyUp))
        }

        runOnIdle {
            assertEquals(true, state)
        }

        onNodeWithTag("toggle").apply {
            performKeyPress(keyEvent(Key.Spacebar, KeyEventType.KeyDown))
            performKeyPress(keyEvent(Key.Spacebar, KeyEventType.KeyUp))
        }

        runOnIdle {
            assertEquals(false, state)
        }
    }
}
