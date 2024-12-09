package org.jetbrains.compose.resources

import kotlinx.cinterop.*
import platform.AppKit.NSScreen
import platform.CoreGraphics.CGDisplayPixelsWide
import platform.CoreGraphics.CGDisplayScreenSize
import platform.Foundation.*

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locale = NSLocale.preferredLanguages.firstOrNull()
        ?.let { NSLocale(it as String) }
        ?: NSLocale.currentLocale

    val languageCode = locale.languageCode
    val regionCode = locale.objectForKey(NSLocaleCountryCode) as? String
    val isDarkTheme = NSUserDefaults.standardUserDefaults.stringForKey("AppleInterfaceStyle") == "Dark"

    val dpi = NSScreen.mainScreen?.let { screen ->
        val backingScaleFactor = screen.backingScaleFactor
        val screenNumber = interpretObjCPointer<NSNumber>(
            screen.deviceDescription["NSScreenNumber"].objcPtr()
        ).unsignedIntValue

        val displaySizePX = CGDisplayPixelsWide(screenNumber).toFloat() * backingScaleFactor
        val displaySizeMM = CGDisplayScreenSize(screenNumber).useContents { width }

        //1 inch = 25.4 mm
        ((displaySizePX / displaySizeMM) * 25.4f).toInt()
    } ?: 0

    return ResourceEnvironment(
        language = LanguageQualifier(languageCode),
        region = RegionQualifier(regionCode.orEmpty()),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}