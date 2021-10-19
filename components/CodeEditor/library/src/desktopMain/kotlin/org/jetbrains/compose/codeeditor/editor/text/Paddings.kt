package org.jetbrains.compose.codeeditor.editor.text

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

internal object Paddings {
    val textFieldLeftPadding = 5.dp
    val lineNumbersLeftPadding = 8.dp
    val lineNumbersRightPadding = 8.dp
    val verticalPadding = 2.dp

    val lineNumbersHorizontalPaddingSum = lineNumbersLeftPadding + lineNumbersRightPadding

    val lineNumbersPadding = PaddingValues.Absolute(
        left = lineNumbersLeftPadding,
        top = verticalPadding,
        right = lineNumbersRightPadding,
        bottom = verticalPadding
    )

    val textFieldPadding = PaddingValues.Absolute(
        left = textFieldLeftPadding,
        top = verticalPadding,
        bottom = verticalPadding
    )
}
