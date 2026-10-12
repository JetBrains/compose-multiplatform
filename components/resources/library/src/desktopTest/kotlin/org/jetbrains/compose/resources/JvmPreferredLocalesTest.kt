package org.jetbrains.compose.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JvmPreferredLocalesTest {
    @Test
    fun parseUnixLanguageTag() {
        val frCa = parseJvmLocaleTag("fr_CA.UTF-8")
        assertNotNull(frCa)
        assertEquals("fr", frCa.language.language)
        assertEquals("CA", frCa.region.region)
    }

    @Test
    fun parseUnixLanguageTagWithModifier() {
        val srLatn = parseJvmLocaleTag("sr_RS@latin")
        assertNotNull(srLatn)
        assertEquals("sr", srLatn.language.language)
        assertEquals("RS", srLatn.region.region)
        // The modifier names the script, so values-b+sr+Latn still matches
        assertEquals("Latn", srLatn.script.script)
    }

    @Test
    fun parseUnixLanguageTagWithCodesetAndModifier() {
        val srLatn = parseJvmLocaleTag("sr_RS.UTF-8@latin")
        assertNotNull(srLatn)
        assertEquals("sr", srLatn.language.language)
        assertEquals("RS", srLatn.region.region)
        assertEquals("Latn", srLatn.script.script)
    }

    @Test
    fun parseUnixLanguageTagWithEveryScriptModifier() {
        // The complete set from glibc localedata/SUPPORTED
        assertEquals("Cyrl", parseJvmLocaleTag("uz_UZ@cyrillic")?.script?.script)
        assertEquals("Deva", parseJvmLocaleTag("sd_IN@devanagari")?.script?.script)
        assertEquals("Latn", parseJvmLocaleTag("tt_RU@iqtelif")?.script?.script)
        assertEquals("Latn", parseJvmLocaleTag("nan_TW@latin")?.script?.script)
    }

    @Test
    fun parseUnixLanguageTagWithNonScriptModifier() {
        val caValencia = parseJvmLocaleTag("ca_ES.UTF-8@valencia")
        assertNotNull(caValencia)
        assertEquals("ca", caValencia.language.language)
        assertEquals("ES", caValencia.region.region)
        assertEquals("", caValencia.script.script)
    }

    @Test
    fun parseUnixCLocaleIsIgnored() {
        assertEquals(null, parseJvmLocaleTag("C"))
        assertEquals(null, parseJvmLocaleTag("POSIX"))
        assertEquals(null, parseJvmLocaleTag(""))
    }

    @Test
    fun parseWindowsRegMultiSz() {
        val output = """
            HKEY_CURRENT_USER\Control Panel\International\User Profile
                Languages    REG_MULTI_SZ    zh-Hans-CN    en-US
        """.trimIndent()
        val locales = parseWindowsLanguagesRegistry(output)
        assertEquals(listOf("zh", "en"), locales.map { it.language.language })
        assertEquals("Hans", locales.first().script.script)
        assertEquals("CN", locales.first().region.region)
    }

    @Test
    fun parseWindowsRegMultiSzEscapedNulls() {
        // The real `reg query` output separates entries with a literal `\0`.
        val output = """
            HKEY_CURRENT_USER\Control Panel\International\User Profile
                Languages    REG_MULTI_SZ    as\0hi\0es-ES\0en-US
        """.trimIndent()
        val locales = parseWindowsLanguagesRegistry(output)
        assertEquals(listOf("as", "hi", "es", "en"), locales.map { it.language.language })
        assertEquals("ES", locales[2].region.region)
    }

    @Test
    fun parseWindowsRegMissingLanguagesValue() {
        val output = """
            HKEY_CURRENT_USER\Control Panel\International\User Profile
        """.trimIndent()
        assertEquals(emptyList(), parseWindowsLanguagesRegistry(output))
    }

    @Test
    fun parseWindowsRegEmptyValue() {
        val output = "    Languages    REG_MULTI_SZ    "
        assertEquals(emptyList(), parseWindowsLanguagesRegistry(output))
    }

    @Test
    fun parseMacosDefaultsLanguages() {
        val output = """
            (
                "it-US",
                "hi-US",
                "es-US",
                "en-US"
            )
        """.trimIndent()
        val locales = parseMacosLanguagesDefaults(output)
        assertEquals(listOf("it", "hi", "es", "en"), locales.map { it.language.language })
        assertEquals(listOf("US", "US", "US", "US"), locales.map { it.region.region })
    }

    @Test
    fun jvmPreferredLocalesIsNeverEmpty() {
        val locales = jvmPreferredLocales()
        assertTrue(locales.isNotEmpty())
        assertTrue(
            locales.all { it.language.language.isNotEmpty() },
            "every preferred locale needs a language: $locales"
        )
    }
}
