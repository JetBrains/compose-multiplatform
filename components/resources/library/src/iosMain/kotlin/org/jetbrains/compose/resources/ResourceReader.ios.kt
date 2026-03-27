package org.jetbrains.compose.resources

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDirectoryEnumerationSkipsHiddenFiles
import platform.Foundation.NSError
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import platform.Foundation.seekToFileOffset
import platform.posix.memcpy

@ExperimentalResourceApi
@OptIn(BetaInteropApi::class)
internal actual fun getPlatformResourceReader(): ResourceReader = DefaultIOsResourceReader

@ExperimentalResourceApi
internal object DefaultIOsResourceReader : ResourceReader {
    private val composeResourcesDir: String by lazy { findComposeResourcesPath() }

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
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                fileHandle.seekToOffset(offset.toULong(), error.ptr)
                error.value?.let { err -> error(err.localizedDescription) }
            }
        } else {
            fileHandle.seekToFileOffset(offset.toULong())
        }
        val result = fileHandle.readDataOfLength(size.toULong())
        fileHandle.closeFile()
        return result
    }

    private fun getPathInBundle(path: String): String {
        return "$composeResourcesDir/$path"
    }

    /**
     * Determines the path to the compose resources directory.
     * It first searches for a "/Frameworks/'*'.framework/composeResources" directory in the main bundle directory.
     * If no such directory exists, it defaults to a directory named "compose-resources" in the main bundle directory.
     *
     * @return The path to the compose resources directory as a string.
     */
    private fun findComposeResourcesPath(): String {
        val mainBundle = NSBundle.mainBundle
        val fm = NSFileManager.defaultManager()
        val frameworkDirs = fm.findSubDirs(mainBundle.resourcePath + "/Frameworks") { it.endsWith(".framework") }
        val frameworkResourcesDir = frameworkDirs.firstOrNull { frameworkDir ->
            fm.findSubDirs(frameworkDir) { it.endsWith("composeResources") }.isNotEmpty()
        }
        val defaultDir = mainBundle.resourcePath + "/compose-resources"
        return frameworkResourcesDir ?: defaultDir
    }

    private fun NSFileManager.findSubDirs(parentDir: String, filter: (String) -> Boolean): List<String> = memScoped {
        if (!fileExistsAtPath(parentDir)) return emptyList()
        val contents = contentsOfDirectoryAtURL(
            url = NSURL(fileURLWithPath = parentDir),
            includingPropertiesForKeys = null,
            options = NSDirectoryEnumerationSkipsHiddenFiles,
            error = null
        ) ?: return emptyList()

        contents.mapNotNull { url ->
            val path = (url as? NSURL)?.path ?: return@mapNotNull null
            val isDir = alloc<BooleanVar>()
            val exist = fileExistsAtPath(path, isDir.ptr)
            if (exist && isDir.value && filter(path)) path else null
        }
    }
}