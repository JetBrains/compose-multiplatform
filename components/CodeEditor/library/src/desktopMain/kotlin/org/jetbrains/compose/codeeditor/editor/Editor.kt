@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")

package org.jetbrains.compose.codeeditor.editor

import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.round
import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionPopup
import org.jetbrains.compose.codeeditor.diagnostics.DiagnosticTooltip
import org.jetbrains.compose.codeeditor.editor.draw.drawHighlights
import org.jetbrains.compose.codeeditor.editor.text.EditorTextField
import org.jetbrains.compose.codeeditor.editor.tooltip.EditorTooltip

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Editor(
    editorState: EditorState,
    onTextChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) = Surface(
    modifier = modifier,
    color = MaterialTheme.colors.background
) {
    Box {
        EditorTextField(
            textState = editorState.textState,
            scrollState = editorState.scrollState,
            onScroll = editorState::onScroll,
            onLineNumbersWidthChange = editorState::onLineNumbersWidthChange,
            modifier = Modifier
                .focusRequester(editorState.focusRequester)
                .onPreviewKeyEvent(editorState.previewKeyEventHandler::onKeyEvent)
                .onKeyEvent(editorState.keyEventHandler::onKeyEvent)
                // handle mouse clicks. clickable, mouseClickable, detectTapGestures don't work with TextField
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            editorState.onClick(awaitPointerEvent(PointerEventPass.Initial))
                        }
                    }
                }
                .pointerMoveFilter(
                    onMove = {
                        editorState.onCursorMove(it.round())
                        false
                    },
                    onExit = {
                        editorState.onCursorMove(OffsetState.Unspecified)
                        false
                    }
                )
                .drawHighlights(
                    editorState.drawState,
                    editorState.scrollState
                )
                .composed { pointerIcon(editorState.cursorPointerIcon) }
        )

        CodeCompletionPopup(
            ccState = editorState.ccState,
            offset = editorState.layoutOffset
        )

        DiagnosticTooltip(
            message = editorState.diagnosticMessagesUnderCursor,
            tooltipState = editorState.diagnosticState.tooltipState
        )

        EditorTooltip(
            tooltipState = editorState.tooltipState
        )

        LaunchedEffect(Unit) {
            editorState.focusRequester.requestFocus()
        }

        LaunchedEffect(editorState.textState.text) {
            onTextChange(editorState.textState.text)
        }
    }
}
