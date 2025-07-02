package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import platform.posix.memcpy

@ExperimentalResourceApi
internal actual fun getPlatformResourceReader(): ResourceReader = DefaultMacOsResourceReader

@ExperimentalResourceApi
internal object DefaultMacOsResourceReader : ResourceReader {
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
        val pathFix = getPathWithoutPackage(path)
        return listOf(
            // Framework binary
            // todo: support fallback path at bundle root?
            NSBundle.mainBundle.resourcePath + "/compose-resources/" + path,
            // Executable binary
            //todo in future bundle resources with app and use all sourceSets (skikoMain, nativeMain)
            "$currentDirectoryPath/src/macosMain/composeResources/$pathFix",
            "$currentDirectoryPath/src/macosTest/composeResources/$pathFix",
            "$currentDirectoryPath/src/commonMain/composeResources/$pathFix",
            "$currentDirectoryPath/src/commonTest/composeResources/$pathFix"
        ).firstOrNull { p -> fm.fileExistsAtPath(p) }  ?: throw MissingResourceException(path)
    }

    private fun getPathWithoutPackage(path: String): String {
        // At the moment resources are not bundled when running a macOS executable binary.
        // As a workaround, load the resources from the actual path on disk. So the
        // "composeResources/PACKAGE/" prefix must be removed. For example:
        // "composeResources/chat_mpp.shared.generated.resources/drawable/background.jpg"
        // Will be transformed into:
        // "drawable/background.jpg"
        // In the future when resources are bundled when running macOS executable binary this
        // workaround is no longer needed.
        require(path.startsWith("composeResources/")) { "Invalid path: $path" }
        return path
            .substringAfter("composeResources/") // remove "composeResources/" part
            .substringAfter("/") // remove PACKAGE path
    }
}