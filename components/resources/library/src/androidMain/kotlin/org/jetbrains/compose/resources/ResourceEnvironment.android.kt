package org.jetbrains.compose.resources

import android.content.res.Configuration
import android.content.res.Resources
import java.util.*

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locale = Locale.getDefault()
    val configuration = Resources.getSystem().configuration
    val isDarkTheme = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val dpi = configuration.densityDpi
    return ResourceEnvironment(
        language = LanguageQualifier(locale.language),
        region = RegionQualifier(locale.country),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}