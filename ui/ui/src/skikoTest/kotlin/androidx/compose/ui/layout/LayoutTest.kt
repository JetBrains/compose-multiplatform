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

package androidx.compose.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalTestApi
class LayoutTest {

    @Test
    // Issue: https://github.com/JetBrains/compose-jb/issues/2696
    fun layoutInMovableContent() = runSkikoComposeUiTest {
        var boxIx by mutableStateOf(0)
        var valueSeenByLayout = -1
        val controlledState = mutableStateOf(0)

        @Composable
        fun ChildContent() {
            Layout(content = {}) { _, _ ->
                layout(1, 1) {
                    valueSeenByLayout = controlledState.value
                }
            }
        }

        setContent {
            val movableChildContent: @Composable () -> Unit = remember {
                movableContentOf { ChildContent() }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (boxIx == 0) {
                    Box(modifier = Modifier.size(200.dp).background(color = Color.Gray)) {
                        movableChildContent()
                    }
                } else if (boxIx == 1) {
                    Box(modifier = Modifier.size(200.dp).background(color = Color.Magenta)) {
                       movableChildContent()
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(0, valueSeenByLayout)

        repeat(5) {
            controlledState.value = it * 2
            waitForIdle()
            assertEquals(
                expected = it * 2,
                actual = valueSeenByLayout,
                message = "valueSeenByLayout should be updated"
            )
        }

        boxIx = 1 // move the child content to another Box

        waitForIdle()
        assertEquals(controlledState.value, valueSeenByLayout)

        repeat(5) {
            controlledState.value = it * 3
            waitForIdle()
            assertEquals(
                expected = it * 3,
                actual = valueSeenByLayout,
                message = "valueSeenByLayout should be updated"
            )
        }
    }
}