package org.jetbrains.compose.codeeditor.editor.text

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.substring
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import org.jetbrains.compose.codeeditor.CodeCompletionElement

@Stable
internal class TextState(
    text: String
) {
    companion object {
        const val INDENT_SIZE = 4
        const val LINE_SEPARATOR = '\n'
    }

    val indent = " ".repeat(INDENT_SIZE)
    val pairChars = mapOf(
        '(' to ')',
        '{' to '}',
        '[' to ']',
        '<' to '>',
        '"' to '"',
        '\'' to '\''
    )

    var text by mutableStateOf(text.replace("\r\n", LINE_SEPARATOR.toString()))
    val highlightedReferenceRanges = mutableStateListOf<TextRange>()
    var caretOffset by mutableStateOf(0)
    var selection by mutableStateOf(TextRange.Zero) // todo: refactor this: remove selection state from EditorTextState
    var textLayoutResult by mutableStateOf<TextLayoutResult?>(null)

    private val boundingBox by derivedStateOf {
        textLayoutResult?.let { layoutResult ->
            if (layoutResult.layoutInput.text.length > 0)
                layoutResult.getBoundingBox(0)
            else null
        }
    }

    val charWidth by derivedStateOf {
        boundingBox?.width ?: 0f
    }
    val lineHeight by derivedStateOf {
        boundingBox?.height ?: 0f
    }

    // todo: add support for selected text
    fun isSelected() = caretOffset == -1

    fun insertIndent() {
        if (isSelected()) return // todo: add support for selected text
        insert(indent)
    }

    fun insertPair(char: Char) {
        if (isSelected()) return // todo: add support for selected text
        val pair = pairChars.getValue(char)
        insert("$char$pair", 1)
    }

    fun paste(pasteOffset: Int, element: CodeCompletionElement) {
        if (element.name.equals("[]")) { // [] for list, string etc.
            text = text.replaceRange(pasteOffset - 1, caretOffset, element.name)
            caretOffset = pasteOffset
        } else if (element.tail != null && element.tail.length >= 2 && element.tail[0] == '(') { // () for functions
            text = text.replaceRange(pasteOffset, caretOffset, element.name + "()")
            var offset = pasteOffset + element.name.length + 1
            if (element.tail[1] == ')') offset++
            caretOffset = offset
        } else {
            text = text.replaceRange(pasteOffset, caretOffset, element.name)
            caretOffset = pasteOffset + element.name.length
        }
    }

    fun checkForRepeating(char: Char, doInsert: Boolean = true): Boolean {
        return when {
            isSelected() -> false // todo: add support for selected text

            caretOffset < text.length && text[caretOffset] == char -> {
                caretOffset++
                true
            }

            doInsert -> {
                insert(char)
                true
            }

            else -> false
        }
    }

    fun removeIndent() {
        if (isSelected()) return // todo: add support for selected text
        val lineOffset = getLineStartOffset()
        var indentOffset = lineOffset
        while (indentOffset < text.length && text[indentOffset] == ' ' && indentOffset - lineOffset < INDENT_SIZE) {
            indentOffset++
        }
        if (indentOffset != lineOffset) {
            text = text.removeRange(lineOffset, indentOffset)
            if (caretOffset in lineOffset until indentOffset) {
                caretOffset = lineOffset
            } else {
                caretOffset -= indentOffset - lineOffset
            }
        }
    }

    fun newLineWithIndent() {
        if (isSelected()) return // todo: add support for selected text
        val preCaretChar = if (caretOffset > 0) text[caretOffset - 1] else ""
        val pairChar = pairChars[preCaretChar]
        if (pairChar != null && pairChar != preCaretChar) { // brackets
            val currentLineIndent = " ".repeat(getLineIndent())
            val preCaretStr = LINE_SEPARATOR + currentLineIndent + indent
            val postCaretStr = if (caretOffset < text.length && text[caretOffset] == pairChar) {
                LINE_SEPARATOR + currentLineIndent
            } else {
                ""
            }
            text = text.replaceRange(caretOffset, caretOffset, preCaretStr + postCaretStr)
            caretOffset += preCaretStr.length
        } else {
            val indentBeforeCaret = " ".repeat(getLineIndent(beforeCaret = true))
            text = text.replaceRange(caretOffset, caretOffset, LINE_SEPARATOR + indentBeforeCaret)
            caretOffset += indentBeforeCaret.length + 1
        }
    }

    fun moveCaretToLineStartWithIndent(): Boolean {
        if (isSelected()) return false // todo: add support for selected text
        val lineIdent = getLineIndent(true)
        return if (lineIdent == caretOffset) false
        else {
            caretOffset = lineIdent
            true
        }
    }

    fun getPrefixBeforeCaret(): Prefix {
        val startOffset = getPrefixStartOffset()
        val prefix = text.substring(startOffset, caretOffset)
        val pasteOffset = if (startOffset < text.length && text[startOffset] == '.') startOffset + 1 else startOffset
        val pastePosition = getPositionForOffset(pasteOffset)
        return Prefix(pasteOffset, pastePosition, prefix)
    }

    fun getCaretPosition() = if (caretOffset != -1) getPositionForOffset(caretOffset) else IntOffset.Zero

    fun getCaretRect() = if (caretOffset != -1) getCursorRectForOffset(caretOffset) else Rect.Zero

    fun getPositionForOffset(offset: Int): IntOffset = getCursorRectForOffset(offset).bottomLeft.round()

    private fun getCursorRectForOffset(offset: Int): Rect = textLayoutResult?.let { layoutResult ->
        if (offset >= layoutResult.layoutInput.text.length) {
            layoutResult.getCursorRect(layoutResult.layoutInput.text.length - 1)
        } else {
            layoutResult.getCursorRect(offset)
        }
    } ?: Rect.Zero

    fun getOffsetForCharacter(lineIndex: Int, characterOffset: Int): Int = textLayoutResult?.let { layoutResult ->
        if (lineIndex >= layoutResult.lineCount)
            layoutResult.getLineStart(layoutResult.lineCount - 1) + characterOffset
        else layoutResult.getLineStart(lineIndex) + characterOffset
    } ?: -1

    fun getOffsetForPosition(cursorPosition: IntOffset): Int {
        return textLayoutResult?.let { layoutResult ->
            if (cursorPosition.x < 0 || cursorPosition.y < 0 || cursorPosition.y > layoutResult.size.height) {
                return -1
            }
            val offsetForPosition = layoutResult.getOffsetForPosition(cursorPosition.toOffset())
            val lineIndex = layoutResult.getLineForOffset(offsetForPosition)
            if (cursorPosition.x > layoutResult.getLineRight(lineIndex)
                || cursorPosition.y > layoutResult.getLineBottom(lineIndex)
                || cursorPosition.y < layoutResult.getLineTop(lineIndex)
            ) {
                return -1
            }
            return offsetForPosition
        } ?: -1
    }

    fun getElementTextRange(offset: Int): TextRange {
        if (offset == -1) return TextRange.Zero
        var startOffset = adjustOffset(
            if (offset >= text.length) text.length - 1 else offset
        )
        var endOffset = startOffset
        var ch = text[startOffset]
        when {
            ch.isElementBoundary() -> return TextRange.Zero

            ch.isJavaIdentifierPart() -> {
                while (startOffset > 0 && text[startOffset - 1].isJavaIdentifierPart()) startOffset--
                while (endOffset < text.length && text[endOffset + 1].isJavaIdentifierPart()) endOffset++
            }

            else -> {
                while (startOffset > 0) {
                    ch = text[startOffset - 1]
                    if (ch.isElementBoundary() || ch.isJavaIdentifierPart()) break
                    startOffset--
                }
                while (endOffset < text.length - 1) {
                    ch = text[endOffset + 1]
                    if (ch.isElementBoundary() || ch.isJavaIdentifierPart()) break
                    endOffset++
                }
            }
        }
        return TextRange(startOffset, endOffset + 1)
    }

    private fun Char.isElementBoundary() = this == '.' || this == ';' || isWhitespace()

    private fun adjustOffset(offset: Int): Int {
        var correctedOffset = offset
        if (!text[correctedOffset].isJavaIdentifierPart()) {
            correctedOffset--
        }
        if (correctedOffset >= 0) {
            val ch = text[correctedOffset]
            if (ch == '\'' || ch == '"' || ch == ')' || ch == ']' || ch.isJavaIdentifierPart()) {
                return correctedOffset
            }
        }
        return offset
    }

    fun clearReferences() {
        if (highlightedReferenceRanges.isNotEmpty()) highlightedReferenceRanges.clear()
    }

    fun highlightReferences(textRange: TextRange) {
        clearReferences()
        when {
            textRange.length == 1 -> {
                highlightedReferenceRanges.add(textRange)
                highlightPairBrackets(textRange, text[textRange.start])
            }

            textRange.length > 1 -> {
                if (text[textRange.start].isJavaIdentifierPart()) {
                    highlightedReferenceRanges.add(textRange)
                } else {
                    for (i in textRange.start until textRange.end) highlightedReferenceRanges.add(TextRange(i, i + 1))
                }
            }
        }
    }

    private fun highlightPairBrackets(textRange: TextRange, rightBracket: Char) {
        val leftBracket = when (rightBracket) {
            ')' -> '('
            ']' -> '['
            else -> return
        }
        var offset = textRange.start - 1
        var bracketsCount = 0
        while (offset > 0) {
            when (text[offset]) {
                rightBracket -> bracketsCount++
                leftBracket -> {
                    if (bracketsCount == 0) break
                    bracketsCount--
                }
            }
            offset--
        }
        if (offset >= 0) highlightedReferenceRanges.add(TextRange(offset, offset + 1))
    }

    fun getLineRight(lineIndex: Int): Float = textLayoutResult?.let { layoutResult ->
        if (lineIndex >= layoutResult.lineCount)
            layoutResult.getLineRight(layoutResult.lineCount - 1)
        else layoutResult.getLineRight(lineIndex)
    } ?: 0f

    fun getLineTop(lineIndex: Int): Float = textLayoutResult?.let { layoutResult ->
        if (lineIndex >= layoutResult.lineCount)
            layoutResult.getLineTop(layoutResult.lineCount - 1)
        else layoutResult.getLineTop(lineIndex)
    } ?: 0f

    fun getLineBottom(lineIndex: Int): Float = textLayoutResult?.let { layoutResult ->
        if (lineIndex >= layoutResult.lineCount)
            layoutResult.getLineBottom(layoutResult.lineCount - 1)
        else layoutResult.getLineBottom(lineIndex)
    } ?: 0f

    fun getLineStart(lineIndex: Int): Int = textLayoutResult?.let { layoutResult ->
        if (lineIndex >= layoutResult.lineCount)
            layoutResult.getLineStart(layoutResult.lineCount - 1)
        else layoutResult.getLineStart(lineIndex)
    } ?: 0

    fun getLineForOffset(offset: Int): Int = textLayoutResult?.let { layoutResult ->
        if (offset >= layoutResult.layoutInput.text.length)
            layoutResult.getLineForOffset(layoutResult.layoutInput.text.length - 1)
        else
            layoutResult.getLineForOffset(offset)
    } ?: 0

    fun getTextRangesOf(str: String): List<TextRange> {
        if (str.isEmpty()) return emptyList()
        var i = 0
        val list = mutableListOf<TextRange>()
        while (true) {
            i = text.indexOf(str, i, true)
            if (i == -1) break
            list.add(TextRange(i, i + str.length))
            i += str.length
        }
        return list
    }

    fun getSelectedText(): String = text.substring(selection)

    fun selectTextRange(textRange: TextRange) {
        selection = textRange
        if (textRange.collapsed) {
            caretOffset = selection.start
        } else {
            caretOffset = -1
        }
    }

    fun unselect() {
        selection = TextRange(selection.end)
        caretOffset = selection.end
    }

    private fun insert(char: Char, offsetShift: Int = 0) = insert(char.toString(), offsetShift)

    private fun insert(str: String, offsetShift: Int = 0) {
        text = text.replaceRange(caretOffset, caretOffset, str)
        caretOffset += str.length - offsetShift
    }

    private fun getLineStartOffset(): Int {
        if (caretOffset == 0) return 0
        val i = text.lastIndexOf(LINE_SEPARATOR, caretOffset - 1)
        return if (i == -1) 0 else i + 1
    }

    private fun getLineIndent(absolute: Boolean = false, beforeCaret: Boolean = false): Int {
        val lineOffset = getLineStartOffset()
        var lineIndent = lineOffset
        while (lineIndent < text.length && text[lineIndent] == ' ' && (!beforeCaret || lineIndent < caretOffset)) {
            lineIndent++
        }
        return if (absolute) lineIndent else lineIndent - lineOffset
    }

    private fun getPrefixStartOffset(): Int {
        var wordStart = caretOffset
        while (wordStart > 0 && text[wordStart - 1].isJavaIdentifierPart()) {
            wordStart--
        }
        if (wordStart - 1 > 0 && text[wordStart - 1] == '.') wordStart--
        return wordStart
    }
}

data class Prefix(
    val offset: Int,
    val position: IntOffset,
    val prefix: String
)
