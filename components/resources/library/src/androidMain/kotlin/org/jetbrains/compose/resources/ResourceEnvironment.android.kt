package org.jetbrains.compose.resources

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import java.util.*

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val configuration = Resources.getSystem().configuration
    val isDarkTheme = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val dpi = configuration.densityDpi
    return ResourceEnvironment(
        locales = getSystemResourceLocales(),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}

@Composable
internal actual fun rememberResourceLocales(): List<ResourceLocale> {
    val configuration = LocalConfiguration.current
    // Android puts the best supported app locale first in the resource configuration.
    // Locale.current instead reads the first device preference, which may be unsupported.
    val locales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales.toResourceLocales()
    } else {
        @Suppress("DEPRECATION")
        listOf(ResourceLocale(ComposeLocale(configuration.locale.toLanguageTag())))
    }
    return remember(locales) { locales }
}

internal actual fun getSystemResourceLocales(): List<ResourceLocale> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // Keep the app-selected locale first while preserving the remaining preferences.
        LocaleList.getAdjustedDefault().toResourceLocales()
    } else {
        listOf(ResourceLocale(ComposeLocale(Locale.getDefault().toLanguageTag())))
    }

private fun LocaleList.toResourceLocales(): List<ResourceLocale> =
    List(size()) { ResourceLocale(ComposeLocale(get(it).toLanguageTag())) }
