package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.util.Locale as JavaLocale

@Composable
internal actual fun getSystemLocales(): List<ResourceLocale> = getSystemResourceLocales()

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locales = getSystemResourceLocales()
    //FIXME: don't use skiko internals
    val isDarkTheme = currentSystemTheme == SystemTheme.DARK
    val dpi = if (GraphicsEnvironment.isHeadless()) {
        // Default to 1x ("unscaled") resources when DPI info not available
        DensityQualifier.MDPI.dpi
    } else {
        Toolkit.getDefaultToolkit().screenResolution
    }
    return ResourceEnvironment(
        locales = locales,
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}

private fun getSystemResourceLocales(): List<ResourceLocale> = listOf(
    ResourceLocale(Locale(JavaLocale.getDefault().toLanguageTag()))
)
