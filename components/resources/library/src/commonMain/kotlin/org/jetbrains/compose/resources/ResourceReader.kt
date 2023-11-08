package org.jetbrains.compose.resources

import androidx.compose.runtime.staticCompositionLocalOf

class MissingResourceException(path: String) : Exception("Missing resource with path: $path")

/**
 * Reads the content of the file at the specified path and returns it as a byte array.
 *
 * @param path The path of the file to read in the resource's directory.
 * @return The content of the file as a byte array.
 */
@ExperimentalResourceApi
expect suspend fun readBytes(path: String): ByteArray

internal interface ResourceReader {
    suspend fun read(path: String): ByteArray
}

internal val DefaultResourceReader: ResourceReader = object : ResourceReader {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun read(path: String): ByteArray = readBytes(path)
}

//ResourceReader provider will be overridden for tests
internal val LocalResourceReader = staticCompositionLocalOf { DefaultResourceReader }
