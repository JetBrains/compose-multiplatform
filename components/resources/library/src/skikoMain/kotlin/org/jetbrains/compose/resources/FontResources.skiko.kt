package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.platform.SystemFont
import androidx.compose.ui.unit.Density

private const val defaultFontIdentity = "org.jetbrains.compose.resources.defaultFont"

// It's mainly necessary for the web.
// A meaningful default font is used while a requested font is being loaded asynchronously.
// Check out skiko for the default font details - it's bundled into skiko.wasm
// https://github.com/JetBrains/skiko/blob/master/skiko/src/webMain/cpp/Roboto-Regular.ttf.cc
//
// Notes:
// - On the web, the default font provided by skiko has limited glyph coverage.
// When encountering unknown glyphs, it will display '�' or tofu (empty box) characters (for example, for emojis).
// - the default font doesn't support font styles and weights customization.
@OptIn(ExperimentalTextApi::class)
private val defaultFont: Font = SystemFont(defaultFontIdentity)

private val fontCache = AsyncCache<String, Font>()

internal val Font.isDefault: Boolean
    @OptIn(ExperimentalTextApi::class)
    get() = (this as? SystemFont)?.identity == defaultFontIdentity


private fun ByteArray.footprint() = "[$size:${lastOrNull()?.toInt()}]"

@Deprecated(
    message = "Use the new Font function with variationSettings instead.",
    level = DeprecationLevel.HIDDEN
)
@Composable
actual fun Font(resource: FontResource, weight: FontWeight, style: FontStyle): Font {
    val resourceReader = LocalResourceReader.currentOrPreview
    val fontFile by rememberResourceState(resource, weight, style, { defaultFont }) { env ->
        val path = resource.getResourceItemByEnvironment(env).path
        val key = "$path:$weight:$style"
        fontCache.getOrLoad(key) {
            val fontBytes = resourceReader.read(path)
            Font("$path${fontBytes.footprint()}", fontBytes, weight, style)
        }
    }
    return fontFile
}
@Composable
actual fun Font(
    resource: FontResource,
    weight: FontWeight,
    style: FontStyle,
    variationSettings: FontVariation.Settings,
): Font {
    val resourceReader = LocalResourceReader.currentOrPreview
    val fontFile by rememberResourceState(resource, weight, style, variationSettings, { defaultFont }) { env ->
        val path = resource.getResourceItemByEnvironment(env).path
        val key = "$path:$weight:$style:${variationSettings.getCacheKey()}"
        fontCache.getOrLoad(key) {
            val fontBytes = resourceReader.read(path)
            Font("$key${fontBytes.footprint()}", fontBytes, weight, style, variationSettings)
        }
    }
    return fontFile
}

internal fun FontVariation.Settings.getCacheKey(): String {
    val defaultDensity = Density(1f)
    return settings
        .map { "${it::class.simpleName}(${it.axisName},${it.toVariationValue(defaultDensity)})" }
        .sorted()
        .joinToString(",")
}