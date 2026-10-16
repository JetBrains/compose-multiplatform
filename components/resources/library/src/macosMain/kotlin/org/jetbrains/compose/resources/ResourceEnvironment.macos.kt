package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.*
import platform.AppKit.NSScreen
import platform.CoreGraphics.CGDisplayPixelsWide
import platform.CoreGraphics.CGDisplayScreenSize
import platform.Foundation.*

@Composable
internal actual fun getSystemLocales(): List<ResourceLocale> {
    val locales = getSystemResourceLocales()
    return remember(locales) { locales }
}

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val locales = getSystemResourceLocales()
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
        locales = locales,
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
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