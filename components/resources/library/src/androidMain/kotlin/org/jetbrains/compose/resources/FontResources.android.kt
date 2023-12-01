package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

@ExperimentalResourceApi
@Composable
actual fun Font(resource: FontResource, weight: FontWeight, style: FontStyle): Font {
    val path = resource.getPathByEnvironment()
    return Font(path, LocalContext.current.assets, weight, style)
}