package org.jetbrains.compose.resources

import kotlinx.browser.window

private external class Intl {
    class Locale(locale: String) {
        val language: String
        val region: String
    }
}

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locale = Intl.Locale(window.navigator.language)
    val isDarkTheme = window.matchMedia("(prefers-color-scheme: dark)").matches
    //96 - standard browser DPI https://developer.mozilla.org/en-US/docs/Web/API/Window/devicePixelRatio
    val dpi: Int = (window.devicePixelRatio * 96).toInt()
    return ResourceEnvironment(
        language = LanguageQualifier(locale.language),
        region = RegionQualifier(locale.region),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}