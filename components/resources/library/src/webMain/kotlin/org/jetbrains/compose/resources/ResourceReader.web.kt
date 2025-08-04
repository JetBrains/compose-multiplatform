package org.jetbrains.compose.resources

internal actual suspend inline fun ResourceReader.readStringItem(path: String, offset: Long, size: Long): ByteArray {
    return (this as WebResourceReader).readStringItem(path, offset, size)
}

internal interface WebResourceReader : ResourceReader {
    suspend fun readStringItem(path: String, offset: Long, size: Long): ByteArray
}