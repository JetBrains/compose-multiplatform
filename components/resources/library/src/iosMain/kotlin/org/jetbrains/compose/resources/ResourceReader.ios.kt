package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun readResourceBytes(path: String): ByteArray {
    val fileManager = NSFileManager.defaultManager()
    val contentsAtPath = fileManager.contentsAtPath(getFilePathInMainBundle(path)) ?: throw MissingResourceException(path)
    return ByteArray(contentsAtPath.length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), contentsAtPath.bytes, contentsAtPath.length)
        }
    }
}

@InternalResourceApi
actual suspend fun convertPathToUri(path: String): String {
    return NSURL.fileURLWithPath(getFilePathInMainBundle(path)).toString()
}

private fun getFilePathInMainBundle(path: String): String {
    // todo: support fallback path at bundle root?
    return "${NSBundle.mainBundle.resourcePath}/compose-resources/$path"
}