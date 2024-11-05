package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Represents the configuration object for web resources.
 *
 * @see configureWebResources - for overriding the default configuration.
 */
@Suppress("unused")
object WebResourcesConfiguration {
    internal var getResourcePath: (path: String) -> String = { "./$it" }

    /**
     * Sets a customization function for resource path. This allows you to modify the resource path
     * before it is used.
     *
     * @param map the mapping function that takes a path String and returns a modified path String
     */
    fun resourcePathMapping(map: (path: String) -> String) {
        getResourcePath = map
    }
}

/**
 * Configures the web resources behavior.
 *
 * Allows users to override default behavior and provide custom logic for generating resource's paths.
 *
 * @param configure Configuration lambda applied to [WebResourcesConfiguration].
 * @see WebResourcesConfiguration For detailed configuration options.
 *
 * Examples:
 * ```
 *  configureWebResources {
 *     resourcePathMapping { path -> "/myApp1/resources/$path" }
 *  }
 *  configureWebResources {
 *     resourcePathMapping { path -> "https://mycdn.com/myApp1/res/$path" }
 *  }
 * ```
 */
@Suppress("unused")
fun configureWebResources(configure: WebResourcesConfiguration.() -> Unit) {
    WebResourcesConfiguration.configure()
}

internal fun getResourceUrl(windowOrigin: String, windowPathname: String, resourcePath: String): String {
    val path = WebResourcesConfiguration.getResourcePath(resourcePath)
    return when {
        path.startsWith("/") -> windowOrigin + path
        path.startsWith("http://") || path.startsWith("https://") -> path
        else -> windowOrigin + windowPathname + path
    }
}

@ExperimentalResourceApi
@Composable
fun preloadAndCacheFont(
    resource: FontResource,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): State<Font?> {
    val fontState = remember(resource, weight, style) { mutableStateOf<Font?>(null) }.apply {
        value = Font(resource, weight, style).takeIf { !it.isDefaultEmptyFont }
    }
    return fontState
}

@ExperimentalResourceApi
@Composable
fun preloadAndCacheImageResource(
    resource: DrawableResource,
): State<ImageBitmap?> {
    val fontState = remember(resource) { mutableStateOf<ImageBitmap?>(null) }.apply {
        value = imageResource(resource).takeIf { !it.isEmptyImageBitmapPlaceholder }
    }
    return fontState
}

@ExperimentalResourceApi
@Composable
fun preloadAndCacheVectorResource(
    resource: DrawableResource,
): State<ImageVector?> {
    val fontState = remember(resource) { mutableStateOf<ImageVector?>(null) }.apply {
        value = vectorResource(resource).takeIf { !it.isEmptyImageVectorPlaceholder }
    }
    return fontState
}