package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

class MissingResourceException(path: String) : Exception("Missing resource with path: $path") {
    internal constructor(path: String, message: String) : this("$path. $message")
}

/**
 * Reads the content of the resource file at the specified path and returns it as a byte array.
 *
 * @param path The path of the file to read in the resource's directory.
 * @return The content of the file as a byte array.
 */
@InternalResourceApi
suspend fun readResourceBytes(path: String): ByteArray = DefaultResourceReader.read(path)

/**
 * Provides the platform dependent URI for a given resource path.
 *
 * @param path The path to the file in the resource's directory.
 * @return The URI string of the specified resource.
 */
@InternalResourceApi
fun getResourceUri(path: String): String = DefaultResourceReader.getUri(path)

@ExperimentalResourceApi
interface ResourceReader {
    suspend fun read(path: String): ByteArray
    suspend fun readPart(path: String, offset: Long, size: Long): ByteArray
    fun getUri(path: String): String
}

internal expect fun getPlatformResourceReader(): ResourceReader

@ExperimentalResourceApi
internal val DefaultResourceReader = getPlatformResourceReader()

//ResourceReader provider will be overridden for tests
@ExperimentalResourceApi
val LocalResourceReader = staticCompositionLocalOf { DefaultResourceReader }

//For an android preview we need to initialize the resource reader with the local context
internal expect val ProvidableCompositionLocal<ResourceReader>.currentOrPreview: ResourceReader
    @Composable get
