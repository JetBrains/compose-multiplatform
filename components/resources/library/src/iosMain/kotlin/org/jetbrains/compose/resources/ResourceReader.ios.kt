package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSInputStream
import platform.Foundation.inputStreamWithFileAtPath
import platform.posix.memcpy

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
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

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual fun getResourceAsFlow(path: String, byteCount: Int): Flow<ByteArray> {
    check(byteCount > 0) { "byteCount: $byteCount" }
    return flow {
        val fileManager = NSFileManager.defaultManager()
        // todo: support fallback path at bundle root?
        val composeResourcesPath = NSBundle.mainBundle.resourcePath + "/compose-resources/" + path
        val stream = fileManager.inputStreamAsPath(composeResourcesPath) ?: throw MissingResourceException(path)
        try {
            stream.open()
            val buffer = ByteArray(byteCount)
            while (true) {
                val numBytesRead = buffer.usePinned { pinned ->
                    stream.read(pinned.addressOf(0).reinterpret(), byteCount.toULong())
                }.toInt()
                when {
                    numBytesRead < 0 -> throw ResourceIOException(
                        stream.streamError?.localizedDescription ?: "Unknown error"
                    )

                    numBytesRead == 0 -> break
                    numBytesRead > 0 -> emit(buffer.sliceArray(0 until numBytesRead))
                }
            }
        } finally {
            stream.close()
        }
    }.flowOn(Dispatchers.IO)
}

private fun NSFileManager.inputStreamAsPath(path: String): NSInputStream? {
    if (!isReadableFileAtPath(path)) {
        return null
    }
    return NSInputStream.inputStreamWithFileAtPath(path)
}