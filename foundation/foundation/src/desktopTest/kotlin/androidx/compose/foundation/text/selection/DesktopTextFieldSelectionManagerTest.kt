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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextFieldState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DesktopTextFieldSelectionManagerTest {
    private val text = "Hello World"
    private val density = Density(density = 1f)
    private val offsetMapping = OffsetMapping.Identity
    private var value = TextFieldValue(text)
    private val lambda: (TextFieldValue) -> Unit = { value = it }
    private val state = TextFieldState(mock())

    private val dragBeginPosition = Offset.Zero
    private val dragLastPosition = Offset(300f, 15f)
    private val beginOffset = 0
    private val dragOffset = text.indexOf('r')
    private val fakeTextRange = TextRange(0, "Hello".length)
    private val dragTextRange = TextRange("Hello".length + 1, text.length)

    private val manager = TextFieldSelectionManager()

    private val clipboardManager = mock<ClipboardManager>()
    private val textToolbar = mock<TextToolbar>()

    @OptIn(InternalFoundationTextApi::class, InternalTextApi::class)
    @Before
    fun setup() {
        manager.offsetMapping = offsetMapping
        manager.onValueChange = lambda
        manager.state = state
        manager.value = value
        manager.clipboardManager = clipboardManager
        manager.textToolbar = textToolbar

        state.layoutResult = mock()
        state.textDelegate = mock()
        whenever(state.layoutResult!!.value).thenReturn(mock())
        whenever(state.textDelegate.density).thenReturn(density)
        whenever(state.layoutResult!!.value.layoutInput).thenReturn(
            TextLayoutInput(
                text = AnnotatedString(text),
                style = TextStyle.Default,
                placeholders = mock(),
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = mock(),
                constraints = Constraints()
            )
        )
        whenever(state.layoutResult!!.getOffsetForPosition(dragBeginPosition)).thenReturn(
            beginOffset
        )
        whenever(state.layoutResult!!.getOffsetForPosition(dragLastPosition)).thenReturn(dragOffset)

        state.processor.reset(value, null, state.inputSession)
    }

    @Test
    fun TextFieldSelectionManager_mouseSelectionObserver_onStart() {
        manager.mouseSelectionObserver { }.onStart(dragBeginPosition)

        assertThat(value.selection).isEqualTo(TextRange(0, 0))

        manager.mouseSelectionObserver { }.onStart(dragLastPosition)
        assertThat(value.selection).isEqualTo(TextRange(8, 8))
    }

    @Test
    fun TextFieldSelectionManager_mouseSelectionObserver_onDrag() {
        val observer = manager.mouseSelectionObserver { }
        observer.onStart(dragBeginPosition)
        observer.onDrag(dragLastPosition)

        assertThat(value.selection).isEqualTo(TextRange(0, 8))
    }

    @Test
    fun TextFieldSelectionManager_mouseSelectionObserver_copy() {
        val observer = manager.mouseSelectionObserver { }
        observer.onStart(dragBeginPosition)
        observer.onDrag(dragLastPosition)

        manager.value = value
        manager.copy(cancelSelection = false)

        verify(clipboardManager, times(1)).setText(AnnotatedString("Hello Wo"))
        assertThat(value.selection).isEqualTo(TextRange(0, 8))
    }
}