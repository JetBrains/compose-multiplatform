package org.jetbrains.compose.codeeditor.gtd

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.codeeditor.GotoDeclarationData
import org.jetbrains.compose.codeeditor.ProjectFile
import org.jetbrains.compose.codeeditor.editor.tooltip.EditorTooltipState
import org.jetbrains.compose.codeeditor.statusbar.BusyState
import java.nio.file.Path

@Stable
internal class GtdState(
    private val scope: CoroutineScope,
    private val projectFile: ProjectFile,
    private val localGoto: (Int) -> Unit,
    private val outerGoto: (String, String, Int) -> Unit,
    private val tooltipState: EditorTooltipState,
    private val busyState: BusyState
) {
    private var getGtdDataDeferred: Deferred<GotoDeclarationData>? = null
    private var initialElementTextRange: TextRange? = null

    fun initialElementContains(offset: Int): Boolean {
        initialElementTextRange?.let {
            if (offset >= it.start && offset <= it.end) return true
        }
        return false
    }

    fun runGtdDataGetter(
        text: String,
        offset: Int,
        range: TextRange?,
        highlight: ((TextRange?) -> Unit)? = null
    ) {
        stop()
        highlight?.invoke(null)
        if (offset == -1) return
        initialElementTextRange = range
        getGtdDataDeferred = scope.async {
            busyState.busy()
            withContext(Dispatchers.IO) {
                val gtdData = projectFile.getGotoDeclarationData(text, offset)
                initialElementTextRange =
                    if (gtdData.isInitialElementOffsetSet)
                        TextRange(gtdData.initialElementStartOffset, gtdData.initialElementEndOffset)
                    else null
                highlight?.invoke(if (gtdData.canNavigate()) initialElementTextRange else null)
                gtdData
            }
        }.apply {
            invokeOnCompletion {
                scope.launch {
                    busyState.free()
                }
            }
        }
    }

    fun goto() {
        scope.launch {
            getGtdDataDeferred?.await()?.let { goto(it) }
        }
    }

    fun goto(text: String, offset: Int, range: TextRange) {
        if (initialElementContains(offset)) return
        runGtdDataGetter(text, offset, range)
        scope.launch {
            getGtdDataDeferred?.await()?.let { goto(it) }
        }
    }

    private fun goto(data: GotoDeclarationData) {
        if (data.isIndexNotReady) {
            tooltipState.show("Navigation is not available during index update")
            return
        }
        if (!data.canNavigate()) {
            tooltipState.show("Cannot find declaration to go to")
            return
        }

        val targets = data.targets
        if (targets.size > 1) {
            /*logger.warn {
                "GotoDeclaration: more than one target. " +
                    "File: ${projectFile.absoluteFilePath}, " +
                    "range: (${data.initialElementStartOffset}, ${data.initialElementEndOffset}), " +
                    "targets: ${targets}"
            }*/
        }
        val target = targets.first()
        if (target.path == projectFile.relativeFilePath) {
            localGoto(target.offset)
        } else {
            if (!Path.of(target.path).isAbsolute) {
                outerGoto(projectFile.projectDir!!, target.path, target.offset)
            } else {
                // todo open other files in read only mode
            }
        }
    }

    fun stop() {
        getGtdDataDeferred?.cancel()
        getGtdDataDeferred = null
        initialElementTextRange = null
    }
}
