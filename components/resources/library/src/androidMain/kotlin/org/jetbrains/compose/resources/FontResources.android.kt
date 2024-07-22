package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*

@Composable
actual fun Font(resource: FontResource, weight: FontWeight, style: FontStyle): Font {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val path = remember(environment, resource) { resource.getResourceItemByEnvironment(environment).path }
    val assets = LocalContext.current.assets
    return Font(path, assets, weight, style)
}