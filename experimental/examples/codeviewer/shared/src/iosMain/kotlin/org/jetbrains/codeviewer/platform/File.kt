@file:Suppress("NewApi")

package org.jetbrains.codeviewer.platform

import kotlinx.coroutines.CoroutineScope
import org.jetbrains.codeviewer.util.TextLines

actual val HomeFolder: File get() = object: File {
    override val name: String
        get() = TODO("Not yet implemented")
    override val isDirectory: Boolean
        get() = TODO("Not yet implemented")
    override val children: List<File>
        get() = TODO("Not yet implemented")
    override val hasChildren: Boolean
        get() = TODO("Not yet implemented")

    override fun readLines(scope: CoroutineScope): TextLines {
        TODO("Not yet implemented")
    }

}