package org.jetbrains.compose.resources

import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.util.*

// Locale.getDefault() holds a single locale and cannot express a fallback chain.
internal actual fun systemPreferredLocales(): List<LocaleQualifiers> =
    listOf(localeQualifiersOf(Locale.getDefault()))

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    //FIXME: don't use skiko internals
    val isDarkTheme = currentSystemTheme == SystemTheme.DARK
    val dpi = if (GraphicsEnvironment.isHeadless()) {
        // Default to 1x ("unscaled") resources when DPI info not available
        DensityQualifier.MDPI.dpi
    } else {
        Toolkit.getDefaultToolkit().screenResolution
    }
    return ResourceEnvironment(
        locales = systemPreferredLocales(),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}

private fun localeQualifiersOf(locale: Locale) = LocaleQualifiers(
    language = LanguageQualifier(locale.language),
    script = ScriptQualifier(locale.script.orEmpty()),
    region = RegionQualifier(locale.country.orEmpty())
)
