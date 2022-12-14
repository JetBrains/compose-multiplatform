package org.jetbrains.codeviewer.platform

import kotlinx.coroutines.CoroutineScope
import org.jetbrains.codeviewer.util.TextLines

expect val HomeFolder: File

interface File {
    val name: String
    val isDirectory: Boolean
    val children: List<File>
    val hasChildren: Boolean

    fun readLines(scope: CoroutineScope): TextLines
}