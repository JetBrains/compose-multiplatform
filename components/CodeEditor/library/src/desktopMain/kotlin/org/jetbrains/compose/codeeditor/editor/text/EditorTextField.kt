package org.jetbrains.compose.codeeditor.editor.text

import org.jetbrains.compose.codeeditor.AppTheme
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import org.jetbrains.compose.fork.text.CoreTextField

@Composable
internal fun EditorTextField(
    textState: TextState,
    scrollState: ScrollState,
    onScroll: (Float) -> Unit = {},
    onLineNumbersWidthChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textFieldState = remember { EditorTextFieldState(textState) }

    Row {
        LineNumbers(
            textState = textState,
            textFieldState = textFieldState,
            scrollState = scrollState,
            onWidthChange = onLineNumbersWidthChange
        )

        TextField(
            textFieldState = textFieldState,
            onScroll = onScroll,
            modifier = modifier
        )
    }
}

@Composable
private fun TextField(
    textFieldState: EditorTextFieldState,
    onScroll: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalTextSelectionColors provides AppTheme.colors.selectionColors) {
        CoreTextField(
            value = textFieldState.textFieldValue,
            onValueChange = textFieldState::onTextFieldValueChange,
            onTextLayout = textFieldState::onTextLayoutChange,
            onScroll = onScroll,
            modifier = Modifier
                .fillMaxSize()
                .padding(Paddings.textFieldPadding)
                .then(modifier),
            textStyle = MaterialTheme.typography.body1,
            cursorBrush = SolidColor(MaterialTheme.colors.onBackground)
        )
    }

}
