package org.jetbrains.compose.resources

import kotlinx.browser.window

private external class Intl {
    class Locale(locale: String) {
        val language: String

        // Intl.Locale.script can be undefined.
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Locale/script
        val script: String?

        // Intl.Locale.region can be undefined.
        // For example, new Int.Locale('en') instead of new Int.Locale('en-NL').
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Locale/region
        val region: String?
    }
}

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locales = getSystemResourceLocales()
    val isDarkTheme = window.matchMedia("(prefers-color-scheme: dark)").matches
    //96 - standard browser DPI https://developer.mozilla.org/en-US/docs/Web/API/Window/devicePixelRatio
    val dpi: Int = (window.devicePixelRatio * 96).toInt()
    return ResourceEnvironment(
        locales = locales,
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}


internal actual fun getSystemResourceLocales(): List<ResourceLocale> =
    window.navigator.languages.asList().ifEmpty { listOf(window.navigator.language) }.map { tag ->
        val locale = Intl.Locale(tag)
        ResourceLocale(
            LanguageQualifier(locale.language),
            ScriptQualifier(locale.script.orEmpty()),
            RegionQualifier(locale.region.orEmpty())
        )
    }
