package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

internal actual fun getPlatformResourceReader(): ResourceReader = object : ResourceReader {
    override suspend fun read(path: String): ByteArray {
        val data = readData(getPathOnDisk(path))
        return ByteArray(data.length.toInt()).apply {
            usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        }
    }

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
        val data = readData(getPathOnDisk(path), offset, size)
        return ByteArray(data.length.toInt()).apply {
            usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        }
    }

    override fun getUri(path: String): String {
        return NSURL.fileURLWithPath(getPathOnDisk(path)).toString()
    }

    private fun readData(path: String): NSData {
        return NSFileManager.defaultManager().contentsAtPath(path) ?: throw MissingResourceException(path)
    }

    private fun readData(path: String, offset: Long, size: Long): NSData {
        val fileHandle = NSFileHandle.fileHandleForReadingAtPath(path) ?: throw MissingResourceException(path)
        fileHandle.seekToOffset(offset.toULong(), null)
        val result = fileHandle.readDataOfLength(size.toULong())
        fileHandle.closeFile()
        return result
    }

    private fun getPathOnDisk(path: String): String {
        val fm = NSFileManager.defaultManager()
        val currentDirectoryPath = fm.currentDirectoryPath
        return listOf(
            //todo in future bundle resources with app and use all sourceSets (skikoMain, nativeMain)
            "$currentDirectoryPath/src/macosMain/composeResources/$path",
            "$currentDirectoryPath/src/macosTest/composeResources/$path",
            "$currentDirectoryPath/src/commonMain/composeResources/$path",
            "$currentDirectoryPath/src/commonTest/composeResources/$path"
        ).firstOrNull { p -> fm.fileExistsAtPath(p) }  ?: throw MissingResourceException(path)
    }
}