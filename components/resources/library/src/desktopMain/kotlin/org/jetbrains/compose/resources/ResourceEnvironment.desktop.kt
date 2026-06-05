package org.jetbrains.compose.resources

import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.util.*

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locale = Locale.getDefault()
    //FIXME: don't use skiko internals
    val isDarkTheme = currentSystemTheme == SystemTheme.DARK
    val dpi = if (GraphicsEnvironment.isHeadless()) {
        // Default to 1x ("unscaled") resources when DPI info not available
        DensityQualifier.MDPI.dpi
    } else {
        Toolkit.getDefaultToolkit().screenResolution
    }
    return ResourceEnvironment(
        language = LanguageQualifier(locale.language),
        script = ScriptQualifier(locale.script),
        region = RegionQualifier(locale.country),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}

internal actual fun androidx.compose.ui.text.intl.Locale.getScript(): String {
    val script = platformLocale.script
    if (script.isNotEmpty()) return script

    val defaultLocale = java.util.Locale.getDefault()
    if (language == defaultLocale.language) {
        return defaultLocale.script
    }
    return ""
}
