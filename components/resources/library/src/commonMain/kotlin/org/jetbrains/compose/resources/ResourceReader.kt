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
suspend fun readResourceBytes(path: String): ByteArray = DefaultResourceReader.read(path)

internal interface ResourceReader {
    suspend fun read(path: String): ByteArray
    suspend fun readPart(path: String, offset: Long, size: Long): ByteArray
}

internal expect fun getPlatformResourceReader(): ResourceReader

internal val DefaultResourceReader = getPlatformResourceReader()

//ResourceReader provider will be overridden for tests
internal val LocalResourceReader = staticCompositionLocalOf { DefaultResourceReader }
