package org.jetbrains.compose.codeeditor.editor.draw

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.codeeditor.editor.text.TextState
import java.util.TreeMap

@Stable
internal class DrawState(
    private val textState: TextState
) {
    private val lineHeight = derivedStateOf { textState.lineHeight }

    val drawers = TreeMap<HighlightDrawer, State<List<LineSegment>>>()

    fun putLineSegments(drawer: HighlightDrawer, lineSegments: State<List<LineSegment>>) {
        drawers[drawer] = lineSegments
    }

    fun removeDrawer(drawer: HighlightDrawer) {
        drawers.remove(drawer)
    }

    fun createWavedLineDrawer(color: Color, zIndex: Int): WavedLineDrawer = WavedLineDrawer(color, zIndex)

    fun createBackgroundDrawer(
        backgroundColor: Color? = null,
        borderColor: Color? = null,
        zIndex: Int
    ): BackgroundDrawer = BackgroundDrawer(backgroundColor, borderColor, lineHeight, zIndex)

    fun getLineSegments(startTextOffset: Int, endTextOffset: Int): List<LineSegment> {
        val startLineIndex = textState.getLineForOffset(startTextOffset)
        val endLineIndex = textState.getLineForOffset(endTextOffset)
        val startCharacter = startTextOffset - textState.getLineStart(startLineIndex)
        val endCharacter = endTextOffset - textState.getLineStart(endLineIndex)
        return getLineSegments(startCharacter, endCharacter, startLineIndex + 1, endLineIndex + 1)
    }

    fun getLineSegments(startCharacter: Int, endCharacter: Int, startLine: Int, endLine: Int): List<LineSegment> {
        return if (startLine == endLine) {
            listOf(lineSegment(startCharacter, endCharacter, startLine))
        } else {
            val list = mutableListOf<LineSegment>()
            list.add(lineSegment(startCharacter, startLine))
            var line = startLine + 1
            while (line < endLine) {
                list.add(lineSegment(line = line))
                line++
            }
            list.add(lineSegment(endCharacter = endCharacter, line = endLine))
            list
        }
    }

    private fun lineSegment(startCharacter: Int = 0, endCharacter: Int, line: Int): LineSegment =
        LineSegment(
            startCharacter * textState.charWidth,
            endCharacter * textState.charWidth,
            textState.getLineBottom(line - 1)
        )

    private fun lineSegment(startCharacter: Int = 0, line: Int): LineSegment =
        LineSegment(
            startCharacter * textState.charWidth,
            textState.getLineRight(line - 1),
            textState.getLineBottom(line - 1)
        )
}

internal fun Modifier.drawHighlights(
    drawState: DrawState,
    scrollState: ScrollState
) = drawWithCache {
    val scrollOffset = scrollState.value

    onDrawBehind {
        drawState.drawers.entries.forEach { (drawer, listState) ->
            drawer.draw(
                lineSegments =
                if (scrollOffset == 0) listState.value
                else listState.value.map { it.copy(y = it.y - scrollOffset) },
                drawScope = this
            )
        }
    }
}
