package org.jetbrains.compose.resources

internal data class WindowLocation(val origin: String, val pathname: String)
internal expect fun getWindowLocation(): WindowLocation

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual fun getResourceUriString(path: String): String {
    val resPath = WebResourcesConfiguration.getResourcePath(path)
    return getWindowLocation().let {
        if (resPath.startsWith("/")) {
            it.origin + resPath
        } else if (resPath.startsWith("http://") || resPath.startsWith("https://")) {
            resPath
        } else {
            it.origin  + it.pathname + resPath
        }
    }
}