package org.jetbrains.compose.resources

import androidx.compose.runtime.staticCompositionLocalOf

@ExperimentalResourceApi
class MissingResourceException(path: String) : Exception("Missing resource with path: $path")

/**
 * Reads the content of the resource file at the specified path and returns it as a byte array.
 *
 * @param path The path of the file to read in the resource's directory.
 * @return The content of the file as a byte array.
 */
@InternalResourceApi
expect suspend fun readResourceBytes(path: String): ByteArray

internal interface ResourceReader {
    suspend fun read(path: String): ByteArray
    suspend fun readPart(path: String, offset: Long, size: Long): ByteArray
}

internal val DefaultResourceReader: ResourceReader = object : ResourceReader {
    @OptIn(InternalResourceApi::class)
    override suspend fun read(path: String): ByteArray = readResourceBytes(path)

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
        TODO("Not yet implemented")
    }
}

//ResourceReader provider will be overridden for tests
internal val LocalResourceReader = staticCompositionLocalOf { DefaultResourceReader }
