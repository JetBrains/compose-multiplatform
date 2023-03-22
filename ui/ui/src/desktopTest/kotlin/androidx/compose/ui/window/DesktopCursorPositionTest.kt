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

package androidx.compose.ui.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
internal class DesktopCursorPositionTest {
    @get:Rule
    val rule = createComposeRule()

    private val windowSize = IntSize(200, 200)
    private val anchorPosition = IntOffset(0, 0)
    private val anchorSize = IntSize(100, 100)
    private val popupSize = IntSize(20, 20)

    // https://github.com/JetBrains/compose-jb/issues/2821
    @Test
    fun `pointer position with single component`(): Unit = runBlocking(Dispatchers.Main) {
        var pointerPosition: IntOffset? = null

        rule.setContent {
            var savePointerPosition by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(200.dp, 200.dp)
                    .testTag("testBox")
                    .onPointerEvent(PointerEventType.Enter, onEvent = {
                        savePointerPosition = true
                    })
            ) {
                if (savePointerPosition) {
                    pointerPosition = rememberCursorPositionProvider().calculatePosition(
                        IntRect(anchorPosition, anchorSize),
                        windowSize,
                        LayoutDirection.Ltr,
                        popupSize
                    )
                }
            }
        }

        rule.onNodeWithTag("testBox").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        rule.waitForIdle()
        assertThat(pointerPosition).isEqualTo(IntOffset(30, 40))
    }
}