package org.jetbrains.codeviewer.util

interface TextLines {
    val size: Int
    suspend fun get(index: Int): String
}

object EmptyTextLines : TextLines {
    override val size: Int
        get() = 0

    override suspend fun get(index: Int): String = ""
}