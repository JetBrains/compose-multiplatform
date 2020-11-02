package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.codeviewer.util.TextLines

@Suppress("BlockingMethodInNonBlockingContext")
fun java.io.File.toProjectFile(): File = object : File {
    override val name: String get() = this@toProjectFile.name

    override val isDirectory: Boolean get() = this@toProjectFile.isDirectory

    override val children: List<File>
        get() = this@toProjectFile
            .listFiles()
            .orEmpty()
            .map { it.toProjectFile() }

    override val hasChildren: Boolean
        get() = isDirectory && listFiles()?.size ?: 0 > 0

    override fun readLines(backgroundScope: CoroutineScope) = object : TextLines {
        val lines = ArrayList<String>()

        override var size by mutableStateOf(0)
        override fun get(index: Int) = lines[index]

        init {
            backgroundScope.launch {
                bufferedReader(charset = Charsets.UTF_8).use {
                    it.lineSequence().forEach { line ->
                        lines.add(line)
                        size = lines.size
                    }
                }
            }
        }
    }
}