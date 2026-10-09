package org.jetbrains.compose.resources

import java.util.Locale
import kotlin.test.*

class ResourceLocalesDesktopTest {
    @Test
    fun localeProviderPreservesJvmDefaultAndApplicationOverrides() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("sr-Latn-RS"))
            val locales = getSystemResourceLocales()
            assertEquals(1, locales.size)
            assertEquals("sr", locales.single().language.language)
            assertEquals("Latn", locales.single().script.script)
            assertEquals("RS", locales.single().region.region)
            assertEquals(locales, getSystemEnvironment().locales)
            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            assertEquals("es", getSystemResourceLocales().single().language.language)
        } finally {
            Locale.setDefault(original)
        }
    }
}
