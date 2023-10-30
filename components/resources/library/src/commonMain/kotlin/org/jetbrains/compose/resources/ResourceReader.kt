package org.jetbrains.compose.resources

class MissingResourceException(path: String) : Exception("Missing resource with path: $path")

/**
 * Finds resource file by ID depending on current environment
 */
@ExperimentalResourceApi
expect suspend fun readBytes(path: String): ByteArray
