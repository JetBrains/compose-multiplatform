package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import platform.Foundation.seekToFileOffset
import platform.posix.memcpy

internal actual fun getPlatformResourceReader(): ResourceReader = object : ResourceReader {
    override suspend fun read(path: String): ByteArray {
        val data = readData(getPathInBundle(path))
        return ByteArray(data.length.toInt()).apply {
            usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        }
    }

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
        val data = readData(getPathInBundle(path), offset, size)
        return ByteArray(data.length.toInt()).apply {
            usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        }
    }

    override fun getUri(path: String): String {
        return NSURL.fileURLWithPath(getPathInBundle(path)).toString()
    }

    private fun readData(path: String): NSData {
        return NSFileManager.defaultManager().contentsAtPath(path) ?: throw MissingResourceException(path)
    }

    private fun readData(path: String, offset: Long, size: Long): NSData {
        val fileHandle = NSFileHandle.fileHandleForReadingAtPath(path) ?: throw MissingResourceException(path)
        if (available(OS.Ios to OSVersion(major = 13))) {
            fileHandle.seekToOffset(offset.toULong(), null)
        } else {
            fileHandle.seekToFileOffset(offset.toULong())
        }
        val result = fileHandle.readDataOfLength(size.toULong())
        fileHandle.closeFile()
        return result
    }

    private fun getPathInBundle(path: String): String {
        // todo: support fallback path at bundle root?
        return NSBundle.mainBundle.resourcePath + "/compose-resources/" + path
    }
}