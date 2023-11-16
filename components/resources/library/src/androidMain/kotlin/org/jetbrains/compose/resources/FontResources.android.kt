package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

@ExperimentalResourceApi
@Composable
actual fun Font(id: ResourceId, weight: FontWeight, style: FontStyle): Font {
    val resourceReader = LocalResourceReader.current
    val path by rememberState(id, { "" }) { getPathById(id, resourceReader) }
    return Font(path, LocalContext.current.assets, weight, style)
}