package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import platform.Foundation.NSFileManager
import platform.Foundation.NSInputStream
import platform.Foundation.inputStreamWithFileAtPath
import platform.posix.memcpy

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun readResourceBytes(path: String): ByteArray {
    val currentDirectoryPath = NSFileManager.defaultManager().currentDirectoryPath
    val contentsAtPath = NSFileManager.defaultManager().run {
        //todo in future bundle resources with app and use all sourceSets (skikoMain, nativeMain)
        contentsAtPath("$currentDirectoryPath/src/macosMain/composeResources/$path")
            ?: contentsAtPath("$currentDirectoryPath/src/macosTest/composeResources/$path")
            ?: contentsAtPath("$currentDirectoryPath/src/commonMain/composeResources/$path")
            ?: contentsAtPath("$currentDirectoryPath/src/commonTest/composeResources/$path")
    } ?: throw MissingResourceException(path)
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
        val currentDirectoryPath = NSFileManager.defaultManager().currentDirectoryPath
        val stream = NSFileManager.defaultManager().run {
            //todo in future bundle resources with app and use all sourceSets (skikoMain, nativeMain)
            inputStreamAsPath("$currentDirectoryPath/src/macosMain/composeResources/$path")
                ?: inputStreamAsPath("$currentDirectoryPath/src/macosTest/composeResources/$path")
                ?: inputStreamAsPath("$currentDirectoryPath/src/commonMain/composeResources/$path")
                ?: inputStreamAsPath("$currentDirectoryPath/src/commonTest/composeResources/$path")
        } ?: throw MissingResourceException(path)
        try {
            stream.open()
            val buffer = ByteArray(byteCount)
            while (true) {
                val numBytesRead = buffer.usePinned { pinned ->
                    stream.read(pinned.addressOf(0).reinterpret(), byteCount.toULong())
                }.toInt()
                when {
                    numBytesRead == 0 -> break
                    numBytesRead > 0 -> emit(buffer.sliceArray(0 until numBytesRead))
                    else -> throw ResourceIOException(stream.streamError?.description)
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