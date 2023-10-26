package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

class MissingResourceException(path: String) : Exception("Missing resource with path: $path")

/**
 * Finds resource file by ID depending on current environment
 */
@ExperimentalResourceApi
expect suspend fun readBytes(path: String): ByteArray

@Composable
internal expect fun rememberBytes(id: ResourceId): State<ByteArray>
