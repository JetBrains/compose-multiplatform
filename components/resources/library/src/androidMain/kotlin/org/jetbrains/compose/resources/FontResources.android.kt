package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*

@ExperimentalResourceApi
@Composable
actual fun Font(resource: FontResource, weight: FontWeight, style: FontStyle): Font {
    val environment = rememberEnvironment()
    val path = remember(environment) { resource.getPathByEnvironment(environment) }
    return Font(path, LocalContext.current.assets, weight, style)
}