package org.jetbrains.compose.resources

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow

@ExperimentalResourceApi
class MissingResourceException(path: String) : Exception("Missing resource with path: $path")

@ExperimentalResourceApi
class ResourceIOException : Exception {
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
}

/**
 * Reads the content of the resource file at the specified path and returns it as a byte array.
 *
 * @param path The path of the file to read in the resource's directory.
 * @return The content of the file as a byte array.
 */
@InternalResourceApi
expect suspend fun readResourceBytes(path: String): ByteArray

/**
 * Returns a flow which emits the content of the resource file as byte array chunks. The length of each chunk is not
 * empty and has the length of [byteCount] or smaller. The flow will throw [MissingResourceException] when the resource
 * file is missing or [ResourceIOException] if any IO error occurs. You can catch those with the
 * [catch][kotlinx.coroutines.flow.catch] operator. This function is useful when the resource is too big to be contained
 * in a single [ByteArray].
 *
 * @param path The path of the file to read in the resource's directory.
 * @param byteCount The maximum length of the emitted byte arrays. The flow can emit an array smaller than this length.
 *
 * @return A flow that emits the content of the file as byte sub-arrays.
 *
 * @throws IllegalArgumentException - when [byteCount] is not positive.
 */
@InternalResourceApi
expect fun getResourceAsFlow(path: String, byteCount: Int = 8192): Flow<ByteArray>

internal interface ResourceReader {
    suspend fun read(path: String): ByteArray
}

internal val DefaultResourceReader: ResourceReader = object : ResourceReader {
    @OptIn(InternalResourceApi::class)
    override suspend fun read(path: String): ByteArray = readResourceBytes(path)
}

//ResourceReader provider will be overridden for tests
internal val LocalResourceReader = staticCompositionLocalOf { DefaultResourceReader }
