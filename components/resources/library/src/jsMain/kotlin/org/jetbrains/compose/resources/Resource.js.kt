package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Represents the configuration object for web resources.
 *
 * @see configureWebResources - for overriding the default configuration.
 */
@Suppress("unused")
@ExperimentalResourceApi
object WebResourcesConfiguration {
    internal var resourcePathCustomization: (path: String) -> String = { "./$it" }

    /**
     * Sets a customization function for resource path. This allows you to modify the resource path
     * before it is used.
     *
     * @param customization the customization function that takes a path String and returns a modified path String
     */
    @ExperimentalResourceApi
    fun setResourcePathCustomization(customization: (path: String) -> String) {
        resourcePathCustomization = customization
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
 *     setResourceFactory { path -> "/myApp1/resources/$path" }
 *  }
 *  configureWebResources {
 *     setResourceFactory { path -> "https://mycdn.com/myApp1/res/$path" }
 *  }
 * ```
 */
@Suppress("unused")
@ExperimentalResourceApi
fun configureWebResources(configure: WebResourcesConfiguration.() -> Unit) {
    WebResourcesConfiguration.configure()
}

internal actual val cacheDispatcher: CoroutineDispatcher = Dispatchers.Default