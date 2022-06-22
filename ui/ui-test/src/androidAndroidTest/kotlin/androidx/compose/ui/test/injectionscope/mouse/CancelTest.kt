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

package androidx.compose.ui.test.injectionscope.mouse

import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.injectionscope.mouse.Common.PrimaryButton
import androidx.compose.ui.test.injectionscope.mouse.Common.runMouseInputInjectionTest
import androidx.compose.ui.test.injectionscope.mouse.Common.verifyMouseEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class CancelTest {
    @Test
    fun cancel() = runMouseInputInjectionTest(
        mouseInput = {
            // press the primary button
            press(MouseButton.Primary)
            // cancel the gesture
            cancel()
        },
        eventVerifiers = arrayOf(
            { this.verifyMouseEvent(0, Enter, false, Offset.Zero) },
            { this.verifyMouseEvent(0, Press, true, Offset.Zero, PrimaryButton) },
        )
    )

    @Test
    fun cancel_withoutPress() = runMouseInputInjectionTest(
        mouseInput = {
            expectError<IllegalStateException>(
                expectedMessage = "Cannot send mouse cancel event, no mouse buttons are pressed"
            ) {
                cancel()
            }
        }
    )
}
