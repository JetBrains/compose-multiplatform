package org.jetbrains.compose.resources

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.intl.Locale
import java.util.Locale as JavaLocale

@Composable
internal actual fun getSystemLocales(): List<ResourceLocale> {
    val configuration = LocalConfiguration.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return remember(configuration.locales) {
            configuration.locales.toResourceLocales()
        }
    } else {
        @Suppress("DEPRECATION")
        return remember(configuration.locale) {
            listOf(configuration.locale.toResourceLocale())
        }
    }
}

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

private fun getSystemResourceLocales(): List<ResourceLocale> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocaleList.getAdjustedDefault().toResourceLocales()
    } else {
        listOf(JavaLocale.getDefault().toResourceLocale())
    }

@RequiresApi(Build.VERSION_CODES.N)
private fun LocaleList.toResourceLocales(): List<ResourceLocale> = List(size()) { get(it).toResourceLocale() }
private fun JavaLocale.toResourceLocale() = ResourceLocale(Locale(toLanguageTag()))
