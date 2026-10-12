package org.jetbrains.compose.resources

import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.util.Locale
import java.util.concurrent.TimeUnit

internal actual fun systemPreferredLocales(): List<LocaleQualifiers> = jvmPreferredLocales()

internal actual fun getSystemEnvironment(): ResourceEnvironment {
    //FIXME: don't use skiko internals
    val isDarkTheme = currentSystemTheme == SystemTheme.DARK
    val dpi = if (GraphicsEnvironment.isHeadless()) {
        // Default to 1x ("unscaled") resources when DPI info not available
        DensityQualifier.MDPI.dpi
    } else {
        Toolkit.getDefaultToolkit().screenResolution
    }
    return ResourceEnvironment(
        locales = systemPreferredLocales(),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi)
    )
}

/**
 * The ordered list of locales the user prefers, most preferred first.
 *
 * [Locale.getDefault] holds a single locale, so it cannot express a fallback chain.
 * Windows and Unix keep the full list outside of any JDK API, so they are read directly.
 *
 * The result is resolved once per process: reading it is not free, and neither the JDK nor
 * the OS offers a change notification to invalidate it. Changing the list takes effect on
 * the next launch.
 */
internal fun jvmPreferredLocales(): List<LocaleQualifiers> = preferredLocales

private val preferredLocales: List<LocaleQualifiers> by lazy { loadPreferredLocales() }

private fun loadPreferredLocales(): List<LocaleQualifiers> {
    val fromWindows = windowsPreferredLocales()
    if (fromWindows.isNotEmpty()) return fromWindows

    val fromMac = macosPreferredLocales()
    if (fromMac.isNotEmpty()) return fromMac

    val fromEnv = unixLanguageList()
    if (fromEnv.isNotEmpty()) return fromEnv

    return listOf(localeQualifiersOf(Locale.getDefault()))
}

private const val MACOS_QUERY_TIMEOUT_SECONDS = 2L

private fun macosPreferredLocales(): List<LocaleQualifiers> {
    if (!System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)) {
        return emptyList()
    }
    return try {
        val process = ProcessBuilder("/usr/bin/defaults", "read", "-g", "AppleLanguages")
            .redirectErrorStream(true)
            .start()
        val output = try {
            if (!process.waitFor(MACOS_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return emptyList()
            }
            process.inputStream.bufferedReader().use { it.readText() }
        } finally {
            process.destroyForcibly()
        }
        parseMacosLanguagesDefaults(output)
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun parseMacosLanguagesDefaults(output: String): List<LocaleQualifiers> {
    return output.lineSequence()
        .map { it.trim().trim('"', '\'', ',', '(', ')', ';') }
        .filter { it.isNotEmpty() && !it.contains(' ') }
        .mapNotNull { parseJvmLocaleTag(it) }
        .distinct()
        .toList()
}

private fun unixLanguageList(): List<LocaleQualifiers> {
    val raw = sequenceOf("LANGUAGE", "LC_ALL", "LC_MESSAGES", "LANG")
        .mapNotNull { System.getenv(it)?.takeIf { value -> value.isNotBlank() } }
        .firstOrNull() ?: return emptyList()
    return raw.split(':', ';')
        .mapNotNull { parseJvmLocaleTag(it) }
        .distinct()
}

// The list shown in Settings > Language. GetUserPreferredUILanguages is not equivalent:
// it drops languages that have no installed Windows UI pack.
private const val WINDOWS_USER_PROFILE_KEY = """HKCU\Control Panel\International\User Profile"""
private const val WINDOWS_LANGUAGES_VALUE = "Languages"
private const val WINDOWS_QUERY_TIMEOUT_SECONDS = 2L

private fun windowsPreferredLocales(): List<LocaleQualifiers> {
    if (!System.getProperty("os.name").orEmpty().contains("win", ignoreCase = true)) {
        return emptyList()
    }
    return try {
        // No JDK or public Win32 API returns this list, so it is read from the registry.
        val reg = System.getenv("SystemRoot")?.let { "$it\\System32\\reg.exe" } ?: "reg"
        val process = ProcessBuilder(reg, "query", WINDOWS_USER_PROFILE_KEY, "/v", WINDOWS_LANGUAGES_VALUE)
            .redirectErrorStream(true)
            .start()
        val output = try {
            if (!process.waitFor(WINDOWS_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return emptyList()
            }
            process.inputStream.bufferedReader().use { it.readText() }
        } finally {
            process.destroyForcibly()
        }
        parseWindowsLanguagesRegistry(output)
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun parseWindowsLanguagesRegistry(output: String): List<LocaleQualifiers> {
    val line = output.lineSequence()
        .firstOrNull { it.contains(WINDOWS_LANGUAGES_VALUE) && it.contains("REG_") }
        ?: return emptyList()
    val value = line.substringAfter("REG_MULTI_SZ", missingDelimiterValue = "")
        .ifEmpty { line.substringAfter("REG_SZ", missingDelimiterValue = "") }
        .trim()
    if (value.isEmpty()) return emptyList()
    // `reg query` prints a REG_MULTI_SZ as the literal text `as\0hi\0es-ES`.
    return value.replace("\\0", "\u0000")
        .split('\u0000', ' ', '\t')
        .mapNotNull { parseJvmLocaleTag(it) }
        .distinct()
}

// A glibc locale is language[_territory][.codeset][@modifier]. Most modifiers name a dialect
// or a currency (ca_ES@valencia, de_DE@euro) and carry no script, but these do, and
// values-b+sr+Latn needs it. Every script-bearing modifier in glibc localedata/SUPPORTED:
private val LOCALE_MODIFIER_SCRIPTS = mapOf(
    "cyrillic" to "Cyrl", // uz_UZ@cyrillic
    "devanagari" to "Deva", // ks_IN@devanagari, sd_IN@devanagari
    "iqtelif" to "Latn", // tt_RU@iqtelif, the IQTElif Latin alphabet for Tatar
    "latin" to "Latn", // sr_RS@latin, nan_TW@latin
)

internal fun parseJvmLocaleTag(raw: String): LocaleQualifiers? {
    val trimmed = raw.trim()
    val modifier = trimmed.substringAfter('@', missingDelimiterValue = "") // sr_RS.UTF-8@latin
    val tag = trimmed.substringBefore('@')
        .substringBefore('.') // fr_CA.UTF-8
        .replace('_', '-')
    if (tag.isEmpty() || tag.equals("C", ignoreCase = true) || tag.equals("POSIX", ignoreCase = true)) {
        return null
    }
    val locale = Locale.forLanguageTag(tag)
    if (locale.language.isEmpty()) return null
    return localeQualifiersOf(locale, LOCALE_MODIFIER_SCRIPTS[modifier.lowercase()].orEmpty())
}

private fun localeQualifiersOf(locale: Locale, fallbackScript: String = "") = LocaleQualifiers(
    language = LanguageQualifier(locale.language),
    script = ScriptQualifier(locale.script.orEmpty().ifEmpty { fallbackScript }),
    region = RegionQualifier(locale.country.orEmpty())
)
