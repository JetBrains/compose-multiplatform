package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.posix.memcpy

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun readResourceBytes(path: String): ByteArray {
    val fm = NSFileManager.defaultManager()
    val contentsAtPath = fm.contentsAtPath(getFullFilePath(path)) ?: throw MissingResourceException(path)
    return ByteArray(contentsAtPath.length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), contentsAtPath.bytes, contentsAtPath.length)
        }
    }
}

@InternalResourceApi
actual suspend fun convertPathToUri(path: String): String {
    return NSURL.fileURLWithPath(getFullFilePath(path)).toString()
}

@OptIn(ExperimentalResourceApi::class)
private fun getFullFilePath(path: String): String {
    val fm = NSFileManager.defaultManager()
    val currentDirectoryPath = fm.currentDirectoryPath
    //todo in future bundle resources with app and use all sourceSets (skikoMain, nativeMain)
    val filePath = listOf(
        "$currentDirectoryPath/src/macosMain/composeResources/$path",
        "$currentDirectoryPath/src/macosTest/composeResources/$path",
        "$currentDirectoryPath/src/commonMain/composeResources/$path",
        "$currentDirectoryPath/src/commonTest/composeResources/$path",
    ).firstOrNull { fm.fileExistsAtPath(it) } ?: throw MissingResourceException(path)
    return filePath
}