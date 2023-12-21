package org.jetbrains.compose.resources

import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import java.awt.Toolkit
import java.util.*

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locale = Locale.getDefault()
    //FIXME: don't use skiko internals
    val isDarkTheme = currentSystemTheme == SystemTheme.DARK
    val dpi = Toolkit.getDefaultToolkit().screenResolution
    return ResourceEnvironment(
        language = LanguageQualifier(locale.language),
        region = RegionQualifier(locale.country),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}