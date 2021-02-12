/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.awt.Component
import java.awt.event.InputMethodEvent
import java.text.AttributedString

private object DummyComponent : Component()

@RunWith(JUnit4::class)
class DesktopInputComponentTest {
    @OptIn(InternalTextApi::class)
    @Test
    fun replaceInputMethodText_basic() {
        val processor = EditProcessor()

        val input = DesktopPlatformInput(DummyDesktopComponent)
        val inputService = TextInputService(input)

        val session = inputService.startInput(
            TextFieldValue(),
            ImeOptions.Default,
            processor::apply,
            {}
        )

        processor.reset(TextFieldValue("h"), session)

        val familyEmoji = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66\u200D\uD83D\uDC66"

        input.replaceInputMethodText(
            InputMethodEvent(
                DummyComponent,
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                AttributedString(familyEmoji).iterator,
                11,
                null,
                null
            )
        )

        val buffer = processor.toTextFieldValue()

        Assert.assertEquals("${familyEmoji}h", buffer.text)
        Assert.assertEquals(TextRange(11), buffer.selection)
    }
}