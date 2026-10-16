package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.*

internal actual fun getSystemResourceLocales(): List<ResourceLocale> =
    resourceLocalesForLanguages(NSLocale.preferredLanguages.map { it as String })

internal fun resourceLocalesForLanguages(languages: List<String>): List<ResourceLocale> {
    val locales = languages.map { NSLocale(it) }
        .ifEmpty { listOf(NSLocale.currentLocale) }
    return locales.map { locale ->
        ResourceLocale(
            LanguageQualifier(locale.languageCode),
            ScriptQualifier(locale.scriptCode.orEmpty()),
            RegionQualifier((locale.objectForKey(NSLocaleCountryCode) as? String).orEmpty())
        )
    }
}

@Composable
internal actual fun rememberResourceLocales(): List<ResourceLocale> {
    val locales = getSystemResourceLocales()
    return remember(locales) { locales }
}
