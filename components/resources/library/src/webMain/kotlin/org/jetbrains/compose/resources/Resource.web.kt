package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
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

/**
 * Preloads a font resource and provides a [State] containing the loaded [Font] or `null` if not yet loaded.
 *
 * Internally, it reads font bytes, converts them to a [Font] object, and caches the result, speeding up future
 * accesses to the same font resource when using @Composable Font function.
 *
 * **Usage Example:**
 * ```
 * @Composable
 * fun MyApp() {
 *     val fontState by preloadFont(Res.font.HeavyFont)
 *
 *     if (fontState != null) {
 *         Text(
 *             text = "Hello, World!",
 *             fontFamily = FontFamily(Font(Res.font.HeavyFont)) // the font is taken from the cache
 *         ) else {
 *              Box(modifier = Modifier.fillMaxSize()) {
 *                  CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
 *              }
 *         }
 *     }
 * }
 * ```
 *
 * @param resource The font resource to be used.
 * @param weight The weight of the font. Default value is [FontWeight.Normal].
 * @param style The style of the font. Default value is [FontStyle.Normal].
 * @return A [State]<[Font]?> object that holds the loaded [Font] when available,
 * or `null` if the font is not yet ready.
 */
@ExperimentalResourceApi
@Composable
fun preloadFont(
    resource: FontResource,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): State<Font?> {
    val resState = remember(resource, weight, style) { mutableStateOf<Font?>(null) }.apply {
        value = Font(resource, weight, style).takeIf { !it.isEmptyPlaceholder }
    }
    return resState
}

/**
 * Preloads an image resource and provides a [State] containing the loaded [ImageBitmap] or `null` if not yet loaded.
 *
 * Internally, it reads the resource bytes, converts them to a [ImageBitmap] object, and caches the result,
 * speeding up future accesses to the same resource when using @Composable [imageResource] function.
 *
 * **Usage Example:**
 * ```
 * @Composable
 * fun MyApp() {
 *     val imageState by preloadImageResource(Res.drawable.heavy_drawable)
 *
 *     if (imageState != null) {
 *         Image(painter = painterResource(Res.drawable.heavy_drawable), contentDescription = null)
 *     } else {
 *         Box(modifier = Modifier.fillMaxSize()) {
 *             CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
 *         }
 *     }
 * }
 * ```
 *
 * @param resource The resource to be used.
 * @return A [State]<[ImageBitmap]?> object that holds the loaded [ImageBitmap] when available,
 * or `null` if the resource is not yet ready.
 */
@ExperimentalResourceApi
@Composable
fun preloadImageResource(
    resource: DrawableResource,
): State<ImageBitmap?> {
    val resState = remember(resource) { mutableStateOf<ImageBitmap?>(null) }.apply {
        value = imageResource(resource).takeIf { !it.isEmptyPlaceholder }
    }
    return resState
}


/**
 * Preloads a vector image resource and provides a [State] containing the loaded [ImageVector] or `null` if not yet loaded.
 *
 * Internally, it reads the resource bytes, converts them to a [ImageVector] object, and caches the result,
 * speeding up future accesses to the same resource when using @Composable [vectorResource] function.
 *
 * **Usage Example:**
 * ```
 * @Composable
 * fun MyApp() {
 *     val iconState by preloadVectorResource(Res.drawable.heavy_vector_icon)
 *
 *     if (iconState != null) {
 *         Image(painter = painterResource(Res.drawable.heavy_vector_icon), contentDescription = null)
 *     } else {
 *         Box(modifier = Modifier.fillMaxSize()) {
*              CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
*          }
 *     }
 * }
 * ```
 *
 * @param resource The resource to be used.
 * @return A [State]<[ImageVector]?> object that holds the loaded [ImageVector] when available,
 * or `null` if the resource is not yet ready.
 */
@ExperimentalResourceApi
@Composable
fun preloadVectorResource(
    resource: DrawableResource,
): State<ImageVector?> {
    val resState = remember(resource) { mutableStateOf<ImageVector?>(null) }.apply {
        value = vectorResource(resource).takeIf { !it.isEmptyPlaceholder }
    }
    return resState
}