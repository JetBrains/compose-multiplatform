package org.jetbrains.compose.codeeditor.diagnostics

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import org.jetbrains.compose.codeeditor.editor.text.TextState

@Stable
internal class DiagnosticElementState(
    val message: String,
    val severity: Severity,
    val startLine: Int,
    val startCharacter: Int,
    val endLine: Int,
    val endCharacter: Int,
    private val textState: TextState
) {
    val startTextOffset by derivedStateOf { textState.getOffsetForCharacter(startLine - 1, startCharacter) }
    val endTextOffset by derivedStateOf { textState.getOffsetForCharacter(endLine - 1, endCharacter) }
}
