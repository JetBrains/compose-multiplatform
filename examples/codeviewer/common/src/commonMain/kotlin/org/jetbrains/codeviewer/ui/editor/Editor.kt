package org.jetbrains.codeviewer.ui.editor

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.codeviewer.platform.File
import org.jetbrains.codeviewer.util.SingleSelection
import org.jetbrains.codeviewer.util.afterSet

class Editor(
    val fileName: String,
    val lines: suspend (backgroundScope: CoroutineScope) -> Lines,
) {
    var close: (() -> Unit)? = null
    lateinit var selection: SingleSelection

    val isActive: Boolean
        get() = selection.selected === this

    fun activate() {
        selection.selected = this
    }

    class Line(val number: Int, val content: Content)

    interface Lines {
        val lineNumberDigitCount: Int get() = size.toString().length
        val size: Int
        suspend fun get(index: Int): Line
    }

    class Content(val value: State<String>, val isCode: Boolean)
}

fun Editor(file: File) = Editor(
    fileName = file.name
) { backgroundScope ->
    val textLines = file.readLines(backgroundScope)
    val indexToEditedText = mutableMapOf<Int, String>()
    val isCode = file.name.endsWith(".kt", ignoreCase = true)

    suspend fun content(index: Int): Editor.Content {
        val text = indexToEditedText[index] ?: textLines.get(index)
        val state = mutableStateOf(text).afterSet {
            indexToEditedText[index] = it
        }
        return Editor.Content(state, isCode)
    }

    object : Editor.Lines {
        override val size get() = textLines.size

        override suspend fun get(index: Int) = Editor.Line(
            number = index + 1,
            content = content(index)
        )
    }
}
