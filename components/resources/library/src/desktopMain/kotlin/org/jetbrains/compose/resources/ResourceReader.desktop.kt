package org.jetbrains.compose.resources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.io.InputStream

private object JvmResourceReader

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun readResourceBytes(path: String): ByteArray {
    try {
        return getResourceAsStream(path).readBytes()
    } catch (e: IOException) {
        throw ResourceIOException(e)
    }
}

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual fun getResourceAsFlow(path: String, byteCount: Int): Flow<ByteArray> {
    check(byteCount > 0) { "byteCount: $byteCount" }
    return flow {
        try {
            val resource = getResourceAsStream(path)
            val buffer = ByteArray(byteCount)
            resource.use {
                var numBytesRead: Int
                while (resource.read(buffer).also { numBytesRead = it } != -1) {
                    emit(buffer.sliceArray(0 until numBytesRead))
                }
            }
        } catch (e: IOException) {
            throw ResourceIOException(e)
        }
    }.flowOn(Dispatchers.IO)
}

@OptIn(ExperimentalResourceApi::class)
private fun getResourceAsStream(path: String): InputStream {
    val classLoader = Thread.currentThread().contextClassLoader ?: JvmResourceReader.javaClass.classLoader
    return classLoader.getResourceAsStream(path) ?: throw MissingResourceException(path)
}