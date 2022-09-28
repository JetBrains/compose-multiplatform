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
import androidx.compose.foundation.text.HandleState
import androidx.compose.foundation.text.TextFieldState
import androidx.compose.foundation.text.TextLayoutResultProxy
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.packInts
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

@RunWith(JUnit4::class)
class TextFieldSelectionManagerTest {
    private val text = "Hello World"
    private val density = Density(density = 1f)
    private val offsetMapping = OffsetMapping.Identity
    private var value = TextFieldValue(text)
    private val lambda: (TextFieldValue) -> Unit = { value = it }
    private val spyLambda = spy(lambda)
    private lateinit var state: TextFieldState

    private val dragBeginPosition = Offset.Zero
    private val dragDistance = Offset(300f, 15f)
    private val beginOffset = 0
    private val dragOffset = text.indexOf('r')
    private val fakeTextRange = TextRange(0, "Hello".length)
    private val dragTextRange = TextRange("Hello".length + 1, text.length)
    private val layoutResult: TextLayoutResult = mock()
    private val layoutResultProxy: TextLayoutResultProxy = mock()
    private lateinit var manager: TextFieldSelectionManager

    private val clipboardManager = mock<ClipboardManager>()
    private val textToolbar = mock<TextToolbar>()
    private val hapticFeedback = mock<HapticFeedback>()
    private val focusRequester = mock<FocusRequester>()

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

        whenever(layoutResult.getWordBoundary(beginOffset))
            .thenAnswer(TextRangeAnswer(fakeTextRange))
        whenever(layoutResult.getWordBoundary(dragOffset))
            .thenAnswer(TextRangeAnswer(dragTextRange))
        whenever(layoutResult.getBidiRunDirection(any()))
            .thenReturn(ResolvedTextDirection.Ltr)
        whenever(layoutResult.getBoundingBox(any())).thenReturn(Rect.Zero)
        // left or right handle drag
        whenever(layoutResult.getOffsetForPosition(dragBeginPosition)).thenReturn(beginOffset)
        whenever(layoutResult.getOffsetForPosition(dragBeginPosition + dragDistance))
            .thenReturn(dragOffset)
        // touch drag
        whenever(
            layoutResultProxy.getOffsetForPosition(dragBeginPosition, false)
        ).thenReturn(beginOffset)
        whenever(
            layoutResultProxy.getOffsetForPosition(dragBeginPosition + dragDistance, false)
        ).thenReturn(dragOffset)

        whenever(layoutResultProxy.value).thenReturn(layoutResult)

        state = TextFieldState(mock(), mock())
        state.layoutResult = layoutResultProxy
        manager.state = state
        whenever(state.textDelegate.density).thenReturn(density)
    }

    @Test
    fun TextFieldSelectionManager_init() {
        assertThat(manager.offsetMapping).isEqualTo(offsetMapping)
        assertThat(manager.onValueChange).isEqualTo(lambda)
        assertThat(manager.state).isEqualTo(state)
        assertThat(manager.value).isEqualTo(value)
    }

    @Test
    fun TextFieldSelectionManager_touchSelectionObserver_onLongPress() {
        whenever(layoutResultProxy.isPositionOnText(dragBeginPosition)).thenReturn(true)

        manager.touchSelectionObserver.onStart(dragBeginPosition)

        assertThat(state.handleState).isEqualTo(HandleState.Selection)
        assertThat(state.showFloatingToolbar).isTrue()
        assertThat(value.selection).isEqualTo(fakeTextRange)
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)

        verify(
            focusRequester,
            times(1)
        ).requestFocus()
    }

    @Test
    fun TextFieldSelectionManager_touchSelectionObserver_onLongPress_blank() {
        // Setup
        val fakeLineNumber = 0
        val fakeLineEnd = text.length
        whenever(layoutResultProxy.isPositionOnText(dragBeginPosition)).thenReturn(false)
        whenever(layoutResultProxy.getLineForVerticalPosition(dragBeginPosition.y))
            .thenReturn(fakeLineNumber)
        whenever(layoutResult.getLineLeft(fakeLineNumber))
            .thenReturn(dragBeginPosition.x + 1.0f)
        whenever(layoutResultProxy.getLineEnd(fakeLineNumber)).thenReturn(fakeLineEnd)

        // Act
        manager.touchSelectionObserver.onStart(dragBeginPosition)

        // Assert
        assertThat(state.handleState).isEqualTo(HandleState.Selection)
        assertThat(state.showFloatingToolbar).isTrue()
        assertThat(value.selection).isEqualTo(TextRange(fakeLineEnd))
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)

        verify(
            focusRequester,
            times(1)
        ).requestFocus()
    }

    @Test
    fun TextFieldSelectionManager_touchSelectionObserver_onDrag() {
        manager.touchSelectionObserver.onStart(dragBeginPosition)
        manager.touchSelectionObserver.onDrag(dragDistance)

        assertThat(value.selection).isEqualTo(TextRange(0, text.length))
        assertThat(state.showFloatingToolbar).isFalse()
        verify(
            hapticFeedback,
            times(2)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_touchSelectionObserver_onStop() {
        manager.touchSelectionObserver.onStart(dragBeginPosition)
        manager.touchSelectionObserver.onDrag(dragDistance)

        manager.touchSelectionObserver.onStop()

        assertThat(state.showFloatingToolbar).isTrue()
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onStart_startHandle() {
        manager.handleDragObserver(isStartHandle = true).onStart(Offset.Zero)

        assertThat(manager.draggingHandle).isNotNull()
        assertThat(state.showFloatingToolbar).isFalse()
        verify(spyLambda, times(0)).invoke(any())
        verify(
            hapticFeedback,
            times(0)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onStart_endHandle() {
        manager.handleDragObserver(isStartHandle = false).onStart(Offset.Zero)

        assertThat(manager.draggingHandle).isNotNull()
        assertThat(state.showFloatingToolbar).isFalse()
        verify(spyLambda, times(0)).invoke(any())
        verify(
            hapticFeedback,
            times(0)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onDrag_startHandle() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.handleDragObserver(isStartHandle = true).onDrag(dragDistance)

        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(value.selection).isEqualTo(TextRange(dragOffset, "Hello".length))
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onDrag_endHandle() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.handleDragObserver(isStartHandle = false).onDrag(dragDistance)

        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(value.selection).isEqualTo(TextRange(0, dragOffset))
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_handleDragObserver_onStop() {
        manager.handleDragObserver(false).onStart(Offset.Zero)
        manager.handleDragObserver(false).onDrag(Offset.Zero)

        manager.handleDragObserver(false).onStop()

        assertThat(manager.draggingHandle).isNull()
        assertThat(state.showFloatingToolbar).isTrue()
        verify(
            hapticFeedback,
            times(0)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_cursorDragObserver_onStart() {
        manager.cursorDragObserver().onStart(Offset.Zero)

        assertThat(manager.draggingHandle).isNotNull()
        assertThat(state.showFloatingToolbar).isFalse()
        verify(spyLambda, times(0)).invoke(any())
        verify(
            hapticFeedback,
            times(0)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_cursorDragObserver_onDrag() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.cursorDragObserver().onDrag(dragDistance)

        assertThat(state.showFloatingToolbar).isFalse()
        assertThat(value.selection).isEqualTo(TextRange(dragOffset, dragOffset))
        verify(
            hapticFeedback,
            times(1)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_cursorDragObserver_onDrag_withVisualTransformation() {
        // there is a placeholder after every other char in the original value
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = 2 * offset
            override fun transformedToOriginal(offset: Int) = offset / 2
        }
        manager.value = TextFieldValue(text = "H*e*l*l*o* *W*o*r*l*d", selection = TextRange(0, 0))
        manager.offsetMapping = offsetMapping
        manager.visualTransformation = VisualTransformation { original ->
            TransformedText(
                AnnotatedString(original.indices.map { original[it] }.joinToString("*")),
                offsetMapping
            )
        }

        manager.cursorDragObserver().onDrag(dragDistance)

        assertThat(value.selection).isEqualTo(TextRange(dragOffset / 2, dragOffset / 2))
    }

    @Test
    fun TextFieldSelectionManager_cursorDragObserver_onStop() {
        manager.handleDragObserver(false).onStart(Offset.Zero)
        manager.handleDragObserver(false).onDrag(Offset.Zero)

        manager.cursorDragObserver().onStop()

        assertThat(manager.draggingHandle).isNull()
        assertThat(state.showFloatingToolbar).isFalse()
        verify(
            hapticFeedback,
            times(0)
        ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    @Test
    fun TextFieldSelectionManager_deselect() {
        whenever(textToolbar.status).thenReturn(TextToolbarStatus.Shown)
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.deselect()

        verify(textToolbar, times(1)).hide()
        assertThat(value.selection).isEqualTo(TextRange("Hello".length))
        assertThat(state.handleState).isEqualTo(HandleState.None)
    }

    @Test
    fun copy_selection_collapse() {
        manager.value = TextFieldValue(text = text, selection = TextRange(4, 4))

        manager.copy()

        verify(clipboardManager, times(0)).setText(any())
    }

    @Test
    fun copy_selection_not_null() {
        manager.value = TextFieldValue(text = text, selection = TextRange(0, "Hello".length))

        manager.copy()

        verify(clipboardManager, times(1)).setText(AnnotatedString("Hello"))
        assertThat(value.selection).isEqualTo(TextRange("Hello".length, "Hello".length))
        assertThat(state.handleState).isEqualTo(HandleState.None)
    }

    @Test
    fun copy_selection_reversed() {
        manager.value = TextFieldValue(
            text = text,
            selection = TextRange("Hello".length, "He".length)
        )

        manager.copy()

        verify(clipboardManager, times(1)).setText(AnnotatedString("llo"))
        assertThat(value.selection).isEqualTo(TextRange("Hello".length, "Hello".length))
        assertThat(state.handleState).isEqualTo(HandleState.None)
    }

    @Test
    fun paste_clipBoardManager_null() {
        manager.clipboardManager = null

        manager.paste()

        verify(spyLambda, times(0)).invoke(any())
    }

    @Test
    fun paste_clipBoardManager_empty() {
        whenever(clipboardManager.getText()).thenReturn(null)

        manager.paste()

        verify(spyLambda, times(0)).invoke(any())
    }

    @Test
    fun paste_clipBoardManager_not_empty() {
        whenever(clipboardManager.getText()).thenReturn(AnnotatedString("Hello"))
        manager.value = TextFieldValue(
            text = text,
            selection = TextRange("Hel".length, "Hello Wo".length)
        )

        manager.paste()

        assertThat(value.text).isEqualTo("HelHellorld")
        assertThat(value.selection).isEqualTo(TextRange("Hello Wo".length, "Hello Wo".length))
        assertThat(state.handleState).isEqualTo(HandleState.None)
    }

    @Test
    fun paste_selection_reversed() {
        whenever(clipboardManager.getText()).thenReturn(AnnotatedString("i"))
        manager.value = TextFieldValue(
            text = text,
            selection = TextRange("Hello".length, "H".length)
        )

        manager.paste()

        assertThat(value.text).isEqualTo("Hi World")
        assertThat(value.selection).isEqualTo(TextRange("Hi".length, "Hi".length))
        assertThat(state.handleState).isEqualTo(HandleState.None)
    }

    @Test
    fun cut_selection_collapse() {
        manager.value = TextFieldValue(text = text, selection = TextRange(4, 4))

        manager.cut()

        verify(clipboardManager, times(0)).setText(any())
    }

    @Test
    fun cut_selection_not_null() {
        manager.value = TextFieldValue(
            text = text + text,
            selection = TextRange("Hello".length, text.length)
        )

        manager.cut()

        verify(clipboardManager, times(1)).setText(AnnotatedString(" World"))
        assertThat(value.text).isEqualTo("HelloHello World")
        assertThat(value.selection).isEqualTo(TextRange("Hello".length, "Hello".length))
        assertThat(state.handleState).isEqualTo(HandleState.None)
    }

    @Test
    fun cut_selection_reversed() {
        manager.value = TextFieldValue(
            text = text,
            selection = TextRange("Hello".length, "He".length)
        )

        manager.cut()

        verify(clipboardManager, times(1)).setText(AnnotatedString("llo"))
        assertThat(value.text).isEqualTo("He World")
        assertThat(value.selection).isEqualTo(TextRange("He".length, "He".length))
        assertThat(state.handleState).isEqualTo(HandleState.None)
    }

    @Test
    fun selectAll() {
        manager.value = TextFieldValue(
            text = text,
            selection = TextRange(0)
        )

        manager.selectAll()

        assertThat(value.selection).isEqualTo(TextRange(0, text.length))
    }

    @Test
    fun selectAll_whenPartiallySelected() {
        manager.value = TextFieldValue(
            text = text,
            selection = TextRange(0, 5)
        )

        manager.selectAll()

        assertThat(value.selection).isEqualTo(TextRange(0, text.length))
    }

    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_Clipboard_empty_not_show_paste() {
        manager.value = TextFieldValue(
            text = text + text,
            selection = TextRange("Hello".length, text.length)
        )

        manager.showSelectionToolbar()

        verify(textToolbar, times(1)).showMenu(any(), any(), isNull(), any(), anyOrNull())
    }

    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_selection_collapse_not_show_copy_cut() {
        whenever(clipboardManager.getText()).thenReturn(AnnotatedString(text))
        manager.value = TextFieldValue(
            text = text + text,
            selection = TextRange(0, 0)
        )

        manager.showSelectionToolbar()

        verify(textToolbar, times(1)).showMenu(any(), isNull(), any(), isNull(), anyOrNull())
    }

    @Test
    fun showSelectionToolbar_trigger_textToolbar_showMenu_no_text_show_paste_only() {
        whenever(clipboardManager.getText()).thenReturn(AnnotatedString(text))
        manager.value = TextFieldValue()

        manager.showSelectionToolbar()

        verify(textToolbar, times(1)).showMenu(any(), isNull(), any(), isNull(), isNull())
    }

    @Test
    fun showSelectionToolbar_trigger_textToolbar_no_menu() {
        whenever(clipboardManager.getText()).thenReturn(null)
        manager.value = TextFieldValue()

        manager.showSelectionToolbar()

        verify(textToolbar, times(1)).showMenu(any(), isNull(), isNull(), isNull(), isNull())
    }

    @Test
    fun showSelectionToolbar_passwordTextField_not_show_copy_cut() {
        manager.visualTransformation = PasswordVisualTransformation()
        whenever(clipboardManager.getText()).thenReturn(AnnotatedString(text))
        manager.value = TextFieldValue(text, TextRange(0, 5))

        manager.showSelectionToolbar()

        verify(textToolbar, times(1)).showMenu(any(), isNull(), any(), isNull(), anyOrNull())
    }

    @Test
    fun isTextChanged_text_changed_return_true() {
        manager.touchSelectionObserver.onStart(dragBeginPosition)
        manager.value = TextFieldValue(text + text)

        assertThat(manager.isTextChanged()).isTrue()
    }

    @Test
    fun isTextChanged_text_unchange_return_false() {
        manager.touchSelectionObserver.onStart(dragBeginPosition)

        assertThat(manager.isTextChanged()).isFalse()
    }
}

// This class is a workaround for the bug that mockito can't stub a method returning inline class.
// (https://github.com/nhaarman/mockito-kotlin/issues/309).
internal class TextRangeAnswer(private val textRange: TextRange) : Answer<Any> {
    override fun answer(invocation: InvocationOnMock?): Any =
        packInts(textRange.start, textRange.end)
}
