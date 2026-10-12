package org.jetbrains.compose.resources

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.*

// LocaleList.getDefault() follows a per-app language, which Resources.getSystem() does not:
// that one stays on the device list. Compose reads the same list for LocaleList.current, so
// stringResource and getString cannot disagree.
internal actual fun systemPreferredLocales(): List<LocaleQualifiers> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val localeList = LocaleList.getDefault()
        if (!localeList.isEmpty) {
            return (0 until localeList.size()).map { i -> localeQualifiersOf(localeList.get(i)) }
        }
    }
    return listOf(localeQualifiersOf(Locale.getDefault()))
}

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    val configuration = Resources.getSystem().configuration
    val isDarkTheme = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val dpi = configuration.densityDpi
    return ResourceEnvironment(
        locales = systemPreferredLocales(),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}

private fun localeQualifiersOf(locale: Locale) = LocaleQualifiers(
    language = LanguageQualifier(locale.language.orEmpty()),
    script = ScriptQualifier(locale.script.orEmpty()),
    region = RegionQualifier(locale.country.orEmpty())
)
