package org.jetbrains.codeviewer.util

interface TextLines {
    val size: Int
    fun get(index: Int): String
}