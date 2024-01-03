package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.posix.memcpy

@OptIn(ExperimentalResourceApi::class)
actual suspend fun readResourceBytes(path: String): ByteArray {
    val currentDirectoryPath = NSFileManager.defaultManager().currentDirectoryPath
    val contentsAtPath = NSFileManager.defaultManager().run {
        //todo in future bundle resources with app and use all sourceSets (skikoMain, nativeMain)
        contentsAtPath("$currentDirectoryPath/src/macosMain/resources/$path")
            ?: contentsAtPath("$currentDirectoryPath/src/commonMain/resources/$path")
    } ?: throw MissingResourceException(path)
    return ByteArray(contentsAtPath.length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), contentsAtPath.bytes, contentsAtPath.length)
        }
    }
}