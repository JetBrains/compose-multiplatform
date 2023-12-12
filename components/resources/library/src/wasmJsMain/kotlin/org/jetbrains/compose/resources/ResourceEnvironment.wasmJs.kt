package org.jetbrains.compose.resources

import kotlinx.browser.window

private external class Intl {
    class Locale(locale: String) {
        val language: String
        val region: String
    }
}

internal actual fun getResourceEnvironment(): ResourceEnvironment {
    val locale = Intl.Locale(window.navigator.language)
    val isDarkTheme = window.matchMedia("(prefers-color-scheme: dark)").matches
    val dpi: Int = (window.devicePixelRatio * 96).toInt()
    return ResourceEnvironment(
        language = LanguageQualifier(locale.language),
        region = RegionQualifier(locale.region),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}