package org.jetbrains.compose.resources

import platform.Foundation.*
import kotlin.test.*

class ResourceLocalesNativeTest {
    @Test
    fun convertsAllApplePreferredLanguagesInOrder() {
        val locales = resourceLocalesForLanguages(listOf("ca-ES", "es-ES", "sr-Latn-RS"))
        assertEquals(listOf("ca", "es", "sr"), locales.map { it.language.language })
        assertEquals(listOf("ES", "ES", "RS"), locales.map { it.region.region })
        assertEquals("Latn", locales.last().script.script)
        val resource = StringResource("apple_locale", "apple_locale", setOf(
            ResourceItem(emptySet(), "default", 0, 0),
            ResourceItem(setOf(LanguageQualifier("es")), "es", 0, 0)
        ), setOf("es"))
        assertEquals("es", resource.getResourceItemByEnvironment(
            ResourceEnvironment(locales, ThemeQualifier.LIGHT, DensityQualifier.MDPI)
        ).path)
    }

    @Test
    fun systemProviderUsesFullApplePreferenceList() {
        assertEquals(resourceLocalesForLanguages(NSLocale.preferredLanguages.map { it as String }), getSystemResourceLocales())
    }

    @Test
    fun emptyApplePreferenceListUsesCurrentLocale() {
        val locales = resourceLocalesForLanguages(emptyList())
        assertEquals(1, locales.size)
        assertEquals(NSLocale.currentLocale.languageCode, locales.single().language.language)
    }
}
