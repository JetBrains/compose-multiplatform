package org.jetbrains.compose.codeeditor.diagnostics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Stable
internal class DiagnosticTooltipState(
    private val scope: CoroutineScope
) {
    var isVisible by mutableStateOf(false)
    val placement = TooltipPlacement.CursorPoint(DpOffset(0.dp, 16.dp))
    private val delay = 500L
    private var job: Job? = null
    var message = ""
        private set

    fun setMessage(message: String) {
        if (this.message !== message) {
            if (message.isNotEmpty()) {
                job?.cancel()
                isVisible = false
                job = scope.launch {
                    delay(delay)
                    isVisible = true
                }
            } else {
                hide()
            }
            this.message = message
        }
    }

    fun hide() {
        job?.cancel()
        isVisible = false
    }
}
