package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

class MissingResourceException(path: String) : Exception("Missing resource with path: $path")

/**
 * Reads the content of the resource file at the specified path and returns it as a byte array.
 *
 * @param path The path of the file to read in the resource's directory.
 * @return The content of the file as a byte array.
 */
@InternalResourceApi
suspend fun readResourceBytes(path: String): ByteArray = readResourceBytes(path, DefaultResourceConfiguration)

/**
 * Reads the content of the resource file at the specified path and returns it as a byte array.
 *
 * @param path The path of the file to read in the resource's directory.
 * @return The content of the file as a byte array.
 */
@InternalResourceApi
suspend fun readResourceBytes(path: String, configuration: ResourceConfiguration): ByteArray = getPlatformResourceReader(configuration).read(path)

/**
 * Provides the platform dependent URI for a given resource path.
 *
 * @param path The path to the file in the resource's directory.
 * @return The URI string of the specified resource.
 */
@InternalResourceApi
fun getResourceUri(path: String): String = getResourceUri(path, DefaultResourceConfiguration)

/**
 * Provides the platform dependent URI for a given resource path.
 *
 * @param path The path to the file in the resource's directory.
 * @return The URI string of the specified resource.
 */
@InternalResourceApi
fun getResourceUri(path: String, configuration: ResourceConfiguration): String = getPlatformResourceReader(configuration).getUri(path)

internal interface ResourceReader {
    suspend fun read(path: String): ByteArray
    suspend fun readPart(path: String, offset: Long, size: Long): ByteArray
    fun getUri(path: String): String
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class ResourceConfiguration

internal expect val DefaultResourceConfiguration: ResourceConfiguration

internal expect fun getPlatformResourceReader(configuration: ResourceConfiguration): ResourceReader

internal val DefaultResourceReader = getPlatformResourceReader(DefaultResourceConfiguration)

//ResourceReader provider will be overridden for tests
internal val LocalResourceReader = compositionLocalOf { DefaultResourceReader }

//For an android preview we need to initialize the resource reader with the local context
internal expect val ProvidableCompositionLocal<ResourceReader>.currentOrPreview: ResourceReader
    @Composable get

@Composable
fun ProvideResourceConfiguration(
    resourceConfiguration: ResourceConfiguration,
    content: @Composable ()->Unit,
) {
    val resourceReader = remember(resourceConfiguration) { getPlatformResourceReader(resourceConfiguration) }
    CompositionLocalProvider(
        LocalResourceReader provides resourceReader,
        content = content,
    )
}
