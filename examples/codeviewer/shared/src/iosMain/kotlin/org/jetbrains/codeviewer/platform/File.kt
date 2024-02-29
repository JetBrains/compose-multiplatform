@file:Suppress("NewApi")

package org.jetbrains.codeviewer.platform

import codeviewer.shared.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.codeviewer.util.EmptyTextLines
import org.jetbrains.codeviewer.util.TextLines
import org.jetbrains.compose.resources.ExperimentalResourceApi

class VirtualFile(override val name: String, override val isDirectory: Boolean, val textLines: TextLines, override val children: List<File> = listOf()): File {
    override val hasChildren: Boolean
        get() = children.size > 0

    override fun readLines(scope: CoroutineScope): TextLines = textLines
}

fun ByteArray.toTextLines(): TextLines = object : TextLines {
    val contents = decodeToString().split("\n")

    override val size: Int
        get() = contents.size

    override fun get(index: Int): String = contents[index]
}


@OptIn(ExperimentalResourceApi::class)
actual val HomeFolder: File get() = VirtualFile("files",
    isDirectory = true,
    textLines = EmptyTextLines,
    children = listOf(
        VirtualFile("EditorView.kt",
            isDirectory = false,
            textLines = runBlocking {
                   Res.readBytes("EditorView.kt")
               }.toTextLines()
        )
    )
)
