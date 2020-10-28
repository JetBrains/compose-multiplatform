package org.jetbrains.codeviewer.util

interface TextLines {
    val size: Int
    suspend fun get(index: Int): String
}