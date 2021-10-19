package org.jetbrains.compose.codeeditor.editor.tooltip

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round
import org.jetbrains.compose.codeeditor.editor.OffsetState
import org.jetbrains.compose.codeeditor.editor.text.TextState

@Stable
internal class EditorTooltipState(
    private val textState: TextState,
    private val layoutOffset: OffsetState
) {
    var isVisible by mutableStateOf(false)
        private set

    var message = ""
        private set

    val caretRect by derivedStateOf {
        if (textState.isSelected()) null
        else textState.getCaretRect().toIntRect().translate(layoutOffset.value)
    }

    fun show(message: String) {
        if (isVisible) hide()
        this.message = message
        isVisible = true
    }

    fun hide() {
        message = ""
        isVisible = false
    }
}

private fun Rect.toIntRect() = IntRect(topLeft.round(), bottomRight.round())
