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
}

internal val DefaultResourceReader: ResourceReader = object : ResourceReader {
    @OptIn(InternalResourceApi::class)
    override suspend fun read(path: String): ByteArray = readResourceBytes(path)
}

//ResourceReader provider will be overridden for tests
internal val LocalResourceReader = staticCompositionLocalOf { DefaultResourceReader }

/**
 * Returns the URI string of the resource file at the specified path.
 *
 * @param path The path of the file to get the URI string from in the compose resource's directory.
 * @return The URI string of the file.
 */
@InternalResourceApi
expect fun getResourceUriString(path: String): String
