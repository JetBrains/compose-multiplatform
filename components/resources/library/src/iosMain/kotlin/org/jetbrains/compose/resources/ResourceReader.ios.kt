package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.posix.memcpy

@OptIn(ExperimentalResourceApi::class)
actual suspend fun readResourceBytes(path: String): ByteArray {
    val fileManager = NSFileManager.defaultManager()
    // todo: support fallback path at bundle root?
    val composeResourcesPath = NSBundle.mainBundle.resourcePath + "/compose-resources/" + path
    val contentsAtPath = fileManager.contentsAtPath(composeResourcesPath) ?: throw MissingResourceException(path)
    return ByteArray(contentsAtPath.length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), contentsAtPath.bytes, contentsAtPath.length)
        }
    }
}