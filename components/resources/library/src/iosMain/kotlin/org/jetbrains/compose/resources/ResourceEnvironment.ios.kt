package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages
import platform.Foundation.scriptCode
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

@Composable
internal actual fun getSystemLocales(): List<ResourceLocale> = getSystemResourceLocales()

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locales = getSystemResourceLocales()
    val mainScreen = UIScreen.mainScreen
    val isDarkTheme = mainScreen.traitCollection().userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

    //there is no an API to get a physical screen size and calculate a real DPI
    val density = mainScreen.scale.toFloat()
    return ResourceEnvironment(
        locales = locales,
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByDensity(density)
    )
}

private fun getSystemResourceLocales(): List<ResourceLocale> =
    resourceLocalesForLanguages(NSLocale.preferredLanguages.map { it as String })

private fun resourceLocalesForLanguages(languages: List<String>): List<ResourceLocale> {
    val locales = languages
        .map { NSLocale(it) }
        .ifEmpty { listOf(NSLocale.currentLocale) }
    return locales.map { locale ->
        ResourceLocale(
            LanguageQualifier(locale.languageCode),
            ScriptQualifier(locale.scriptCode.orEmpty()),
            RegionQualifier((locale.objectForKey(NSLocaleCountryCode) as? String).orEmpty())
        )
    }
}