package org.jetbrains.compose.resources

import platform.Foundation.*
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locale = NSLocale.currentLocale()

    val mainScreen = UIScreen.mainScreen
    val isDarkTheme = mainScreen.traitCollection().userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

    //there is no an API to get a physical screen size and calculate a real DPI
    val density = mainScreen.scale.toFloat()
    return ResourceEnvironment(
        language = LanguageQualifier(locale.languageCode),
        region = RegionQualifier(locale.regionCode.orEmpty()),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByDensity(density)
    )
}