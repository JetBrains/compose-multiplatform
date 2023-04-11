package org.jetbrains.codeviewer.util

interface TextLines {
    val size: Int
    fun get(index: Int): String
}

object EmptyTextLines : TextLines {
    override val size: Int
        get() = 0

    override fun get(index: Int): String = ""
}