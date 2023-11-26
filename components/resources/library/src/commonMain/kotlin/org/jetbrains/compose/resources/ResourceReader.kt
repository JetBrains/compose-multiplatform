package org.jetbrains.compose.resources

import androidx.compose.runtime.staticCompositionLocalOf

@ExperimentalResourceApi
class MissingResourceException(path: String) : Exception("Missing resource with path: $path")

/**
 * Reads the content of the resource file at the specified path and returns it as a byte array.
 *
 * @param path The path of the file to read in the resource's directory.
 * @param defaultPath The path of the default file to read in the resource's directory in case path was not found.
 * @return The content of the file as a byte array.
 */
@ExperimentalResourceApi
expect suspend fun readResourceBytes(path: String, defaultPath: String? = null): ByteArray

internal interface ResourceReader {
    suspend fun read(path: String, defaultPath: String? = null): ByteArray
}

internal val DefaultResourceReader: ResourceReader = object : ResourceReader {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun read(path: String, defaultPath: String?): ByteArray =
        readResourceBytes(path,defaultPath)
}

//ResourceReader provider will be overridden for tests
internal val LocalResourceReader = staticCompositionLocalOf { DefaultResourceReader }
