package org.jetbrains.compose.resources

import platform.Foundation.*
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locale = NSLocale.preferredLanguages.firstOrNull()
        ?.let { NSLocale(it as String) }
        ?: NSLocale.currentLocale

    val languageCode = locale.languageCode
    val regionCode = locale.objectForKey(NSLocaleCountryCode) as? String
    val mainScreen = UIScreen.mainScreen
    val isDarkTheme = mainScreen.traitCollection().userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

    //there is no an API to get a physical screen size and calculate a real DPI
    val density = mainScreen.scale.toFloat()
    return ResourceEnvironment(
        language = LanguageQualifier(languageCode),
        region = RegionQualifier(regionCode.orEmpty()),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByDensity(density)
    )
}