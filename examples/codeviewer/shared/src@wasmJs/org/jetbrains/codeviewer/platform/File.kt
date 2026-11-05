package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import codeviewer.shared.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.codeviewer.util.EmptyTextLines
import org.jetbrains.codeviewer.util.TextLines

class VirtualFolder(override val name: String, override val children: List<File>) : File {
    override val isDirectory: Boolean
        get() = true

    override val hasChildren: Boolean
        get() = children.isNotEmpty()

    override fun readLines(scope: CoroutineScope): TextLines = EmptyTextLines
}

class ResourceFile(override val name: String, private val path: String) : File {
    override val isDirectory: Boolean
        get() = false

    override val children: List<File>
        get() = emptyList()

    override val hasChildren: Boolean
        get() = false

    // Resources are fetched over the network on the web, so lines are loaded in background
    override fun readLines(scope: CoroutineScope): TextLines {
        var lines by mutableStateOf(emptyList<String>())

        scope.launch {
            lines = Res.readBytes(path).decodeToString().split("\n")
        }

        return object : TextLines {
            override val size get() = lines.size

            override fun get(index: Int): String = lines[index]
        }
    }
}

actual val HomeFolder: File get() = VirtualFolder("files",
    children = listOf(
        ResourceFile("EditorView.kt", "files/EditorView.kt")
    )
)
