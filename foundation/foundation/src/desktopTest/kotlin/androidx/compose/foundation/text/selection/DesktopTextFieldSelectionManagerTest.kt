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
import androidx.compose.foundation.text.TextLayoutResultProxy
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
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
    private lateinit var state: TextFieldState

    private val dragBeginPosition = Offset.Zero
    private val dragDistance = Offset(300f, 15f)
    private val beginOffset = 0
    private val dragOffset = text.indexOf('r')
    private val layoutResult: TextLayoutResult = mock()
    private val layoutResultProxy: TextLayoutResultProxy = mock()
    private lateinit var manager: TextFieldSelectionManager

    private val clipboardManager = mock<ClipboardManager>()
    private val textToolbar = mock<TextToolbar>()
    private val hapticFeedback = mock<HapticFeedback>()
    private val focusRequester = mock<FocusRequester>()
    private val processor = mock<EditProcessor>()

    @OptIn(InternalFoundationTextApi::class)
    @Before
    fun setup() {
        manager = TextFieldSelectionManager()
        manager.offsetMapping = offsetMapping
        manager.onValueChange = lambda
        manager.value = value
        manager.clipboardManager = clipboardManager
        manager.textToolbar = textToolbar
        manager.hapticFeedBack = hapticFeedback
        manager.focusRequester = focusRequester

        whenever(layoutResult.layoutInput).thenReturn(
            TextLayoutInput(
                text = AnnotatedString(text),
                style = TextStyle.Default,
                placeholders = mock(),
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = mock(),
                constraints = Constraints()
            )
        )

        whenever(layoutResult.getBoundingBox(any())).thenReturn(Rect.Zero)
        whenever(layoutResult.getOffsetForPosition(dragBeginPosition)).thenReturn(beginOffset)
        whenever(layoutResult.getOffsetForPosition(dragBeginPosition + dragDistance))
            .thenReturn(dragOffset)
        whenever(
            layoutResultProxy.getOffsetForPosition(dragBeginPosition, false)
        ).thenReturn(beginOffset)
        whenever(
            layoutResultProxy.getOffsetForPosition(dragBeginPosition + dragDistance, false)
        ).thenReturn(dragOffset)
        whenever(
            layoutResultProxy.getOffsetForPosition(dragBeginPosition + dragDistance)
        ).thenReturn(dragOffset)

        whenever(layoutResultProxy.value).thenReturn(layoutResult)

        state = TextFieldState(mock(), mock())
        state.layoutResult = layoutResultProxy
        state.processor.reset(value, null)
        manager.state = state
        whenever(state.textDelegate.density).thenReturn(density)
    }

    @Test
    fun TextFieldSelectionManager_mouseSelectionObserver_onStart() {
        manager.mouseSelectionObserver.onStart(dragBeginPosition, SelectionAdjustment.None)

        assertThat(value.selection).isEqualTo(TextRange(0, 0))

        manager.mouseSelectionObserver.onStart(
            dragBeginPosition + dragDistance,
            SelectionAdjustment.None
        )
        assertThat(value.selection).isEqualTo(TextRange(8, 8))
    }

    @Test
    fun TextFieldSelectionManager_mouseSelectionObserver_onStart_withShift() {
        manager.mouseSelectionObserver.onExtend(dragBeginPosition)

        assertThat(value.selection).isEqualTo(TextRange(0, 0))

        manager.mouseSelectionObserver.onExtend(dragBeginPosition + dragDistance)
        assertThat(value.selection).isEqualTo(TextRange(0, 8))
    }

    @Test
    fun TextFieldSelectionManager_mouseSelectionObserver_onDrag() {
        val observer = manager.mouseSelectionObserver
        observer.onStart(dragBeginPosition, SelectionAdjustment.None)
        observer.onDrag(dragDistance, SelectionAdjustment.None)

        assertThat(value.selection).isEqualTo(TextRange(0, 8))
    }

    @Test
    fun TextFieldSelectionManager_mouseSelectionObserver_copy() {
        val observer = manager.mouseSelectionObserver
        observer.onStart(dragBeginPosition, SelectionAdjustment.None)
        observer.onDrag(dragDistance, SelectionAdjustment.None)

        manager.value = value
        manager.copy(cancelSelection = false)

        verify(clipboardManager, times(1)).setText(AnnotatedString("Hello Wo"))
        assertThat(value.selection).isEqualTo(TextRange(0, 8))
    }
}