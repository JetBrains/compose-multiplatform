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

// navigator.languages is the preferred-language list; navigator.language is only its first entry.
internal actual fun systemPreferredLocales(): List<LocaleQualifiers> {
    val languages = window.navigator.languages
    if (languages.length == 0) return listOf(localeQualifiersOf(window.navigator.language))
    return (0 until languages.length).map { i -> localeQualifiersOf(languages[i].toString()) }
}

private fun localeQualifiersOf(languageTag: String): LocaleQualifiers {
    val locale = Intl.Locale(languageTag)
    return LocaleQualifiers(
        language = LanguageQualifier(locale.language),
        script = ScriptQualifier(locale.script ?: ""),
        region = RegionQualifier(locale.region ?: "")
    )
}

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locales = systemPreferredLocales()
    val isDarkTheme = window.matchMedia("(prefers-color-scheme: dark)").matches
    //96 - standard browser DPI https://developer.mozilla.org/en-US/docs/Web/API/Window/devicePixelRatio
    val dpi: Int = (window.devicePixelRatio * 96).toInt()
    return ResourceEnvironment(
        locales = locales,
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}