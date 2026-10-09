package org.jetbrains.compose.resources

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import java.util.*

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locale = Locale.getDefault()
    val configuration = Resources.getSystem().configuration
    val isDarkTheme = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val dpi = configuration.densityDpi
    return ResourceEnvironment(
        language = LanguageQualifier(locale.language),
        script = ScriptQualifier(locale.script),
        region = RegionQualifier(locale.country),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}

@Composable
internal actual fun rememberResourceLocale(): ComposeLocale {
    val configuration = LocalConfiguration.current
    // Android puts the best supported app locale first in the resource configuration.
    // Locale.current instead reads the first device preference, which may be unsupported.
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        configuration.locale
    }
    return remember(locale) { ComposeLocale(locale.toLanguageTag()) }
}
