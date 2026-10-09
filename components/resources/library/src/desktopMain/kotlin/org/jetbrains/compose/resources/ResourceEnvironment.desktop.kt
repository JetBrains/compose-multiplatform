package org.jetbrains.compose.resources

import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.util.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale as ComposeLocale

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

internal actual fun getSystemResourceLocales(): List<ResourceLocale> =
    listOf(ResourceLocale(ComposeLocale(Locale.getDefault().toLanguageTag())))

@Composable
internal actual fun rememberResourceLocales(): List<ResourceLocale> {
    val locales = getSystemResourceLocales()
    return remember(locales) { locales }
}
