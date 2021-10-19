package org.jetbrains.compose.codeeditor.editor.tooltip

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun EditorTooltip(
    tooltipState: EditorTooltipState
) {
    if (tooltipState.isVisible) {
        tooltipState.caretRect?.let {
            val positionProvider = EditorTooltipPositionProvider(it, 2)

            Tooltip(
                message = tooltipState.message,
                positionProvider = positionProvider,
                onDismissRequest = tooltipState::hide
            )
        }
    }
}

private class EditorTooltipPositionProvider(
    val caret: IntRect,
    val padding: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val w = popupContentSize.width
        var x = caret.center.x - w / 2
        if (x + w > anchorBounds.right) {
            x = anchorBounds.right - w
        }
        if (x < anchorBounds.left) {
            x = anchorBounds.left
        }

        val h = popupContentSize.height
        var y = caret.bottom + padding
        if (y + h > anchorBounds.bottom) {
            y = caret.top - h - padding
        }

        return IntOffset(x, y)
    }
}
