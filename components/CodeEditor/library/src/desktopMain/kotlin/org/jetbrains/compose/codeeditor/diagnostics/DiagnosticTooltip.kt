package org.jetbrains.compose.codeeditor.diagnostics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import org.jetbrains.compose.codeeditor.editor.tooltip.Tooltip

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun DiagnosticTooltip(
    message: String,
    tooltipState: DiagnosticTooltipState
) {
    tooltipState.setMessage(message)

    if (tooltipState.isVisible) {
        Tooltip(
            message = tooltipState.message,
            positionProvider = tooltipState.placement.positionProvider(),
            onDismissRequest = tooltipState::hide
        )
    }
}

