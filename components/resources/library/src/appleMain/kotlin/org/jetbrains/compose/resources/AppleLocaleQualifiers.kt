package org.jetbrains.compose.resources

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSLocaleLanguageCode
import platform.Foundation.NSLocaleScriptCode
import platform.Foundation.currentLocale
import platform.Foundation.preferredLanguages

// preferredLanguages is the ordered preferred-language list. Unlike
// Bundle.main.preferredLocalizations it is not filtered against Xcode .lproj bundles,
// and it keeps the region, so values-en-rIN still matches a user on en-IN.
internal actual fun systemPreferredLocales(): List<LocaleQualifiers> {
    val tags = NSLocale.preferredLanguages.mapNotNull { it as? String }
    if (tags.isEmpty()) return listOf(localeQualifiersOf(NSLocale.currentLocale))
    return tags.map { localeQualifiersOf(NSLocale(it)) }
}

private fun localeQualifiersOf(loc: NSLocale): LocaleQualifiers {
    val languageCode = (loc.objectForKey(NSLocaleLanguageCode) as? String).orEmpty()
    val scriptCode = (loc.objectForKey(NSLocaleScriptCode) as? String).orEmpty()
    val regionCode = (loc.objectForKey(NSLocaleCountryCode) as? String).orEmpty()
    return LocaleQualifiers(
        language = LanguageQualifier(languageCode),
        script = ScriptQualifier(scriptCode),
        region = RegionQualifier(regionCode)
    )
}
