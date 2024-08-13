@file:OptIn(ExperimentalForeignApi::class)

package example.imageviewer.storage

import kotlinx.cinterop.*
import kotlinx.coroutines.yield
import platform.Foundation.*
import platform.posix.memcpy

val NSFileManager.DocumentDirectory
    get() = URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        create = true,
        appropriateForURL = null,
        error = null
    )!!

// Mimic to java's File class
@Suppress("FunctionName")
fun File(dir: NSURL, child: String) =
    dir.URLByAppendingPathComponent(child)!!

val NSURL.isDirectory: Boolean
    get() {
        return memScoped {
            val isDirectory = alloc<BooleanVar>()
            val fileExists = NSFileManager.defaultManager.fileExistsAtPath(path!!, isDirectory.ptr)
            fileExists && isDirectory.value
        }
    }

fun NSURL.mkdirs() {
    NSFileManager.defaultManager.createDirectoryAtURL(this, true, null, null)
}

fun NSURL.listFiles(filter: (NSURL, String) -> Boolean) =
    NSFileManager.defaultManager.contentsOfDirectoryAtPath(path!!, null)
        ?.map { it.toString() }
        ?.filter { filter(this, it) }
        ?.map { File(this, it) }
        ?.toTypedArray()

fun NSURL.delete() {
    NSFileManager.defaultManager.removeItemAtURL(this, null)
}

suspend fun NSURL.readData(): NSData {
    while (true) {
        val data = NSData.dataWithContentsOfURL(this)
        if (data != null)
            return data
        yield()
    }
}

suspend fun NSURL.readBytes(): ByteArray =
    with(readData()) {
        ByteArray(length.toInt()).apply {
            usePinned {
                memcpy(it.addressOf(0), bytes, length)
            }
        }
    }

fun NSURL.readText(): String =
    NSString.stringWithContentsOfURL(
        url = this,
        encoding = NSUTF8StringEncoding,
        error = null,
    ) as String

fun NSURL.writeText(text: String) {
    (text as NSString).writeToURL(
        url = this,
        atomically = true,
        encoding = NSUTF8StringEncoding,
        error = null
    )
}
