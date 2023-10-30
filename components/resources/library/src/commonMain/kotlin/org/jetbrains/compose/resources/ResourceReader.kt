package org.jetbrains.compose.resources

class MissingResourceException(path: String) : Exception("Missing resource with path: $path")

/**
 * Reads the content of the file at the specified path and returns it as a byte array.
 *
 * @param path The path of the file to read in the resource's directory.
 * @return The content of the file as a byte array.
 */
@ExperimentalResourceApi
expect suspend fun readBytes(path: String): ByteArray
