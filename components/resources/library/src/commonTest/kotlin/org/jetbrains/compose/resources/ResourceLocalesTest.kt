package org.jetbrains.compose.resources

import androidx.compose.ui.text.intl.Locale
import kotlin.test.*

class ResourceLocalesTest {
    private fun environment(vararg tags: String) = ResourceEnvironment(
        tags.map { ResourceLocale(Locale(it)) }, ThemeQualifier.LIGHT, DensityQualifier.MDPI
    )

    private fun resource(supported: Set<String>, vararg translated: String) = StringResource(
        "locale_test", "locale_test",
        setOf(ResourceItem(emptySet(), "default", 0, 0)) + translated.map { tag ->
            val locale = ResourceLocale(Locale(tag))
            ResourceItem(buildSet {
                add(locale.language)
                if (!locale.script.isEmpty()) add(locale.script)
                if (locale.region.region.isNotEmpty()) add(locale.region)
            }, tag, 0, 0)
        }, supported
    )

    @Test
    fun skipsUnsupportedFirstLanguage() {
        assertEquals("es", resource(setOf("es"), "es").getResourceItemByEnvironment(environment("fr", "es")).path)
    }

    @Test
    fun triesAllPreferencesInOrder() {
        val resource = resource(setOf("es", "de"), "es", "de")
        assertEquals("es", resource.getResourceItemByEnvironment(environment("ca", "ru", "es", "de")).path)
        assertEquals("de", resource.getResourceItemByEnvironment(environment("ca", "de", "es")).path)
    }

    @Test
    fun firstSupportedLanguageWinsEvenWithIncompleteTranslation() {
        // French exists in another entry of this module, so this missing entry must use default.
        val resource = resource(setOf("fr", "es"), "es")
        assertEquals("default", resource.getResourceItemByEnvironment(environment("fr", "es")).path)
        assertEquals("es", resource.getResourceItemByEnvironment(environment("es", "fr")).path)
    }

    @Test
    fun defaultEnglishParticipatesInPreferenceOrderLikeAndroid() {
        assertEquals("default", resource(setOf("es"), "es").getResourceItemByEnvironment(environment("en-US", "es")).path)
    }

    @Test
    fun noSupportedPreferenceUsesDefault() {
        assertEquals("default", resource(setOf("es"), "es").getResourceItemByEnvironment(environment("fr", "ca")).path)
    }

    @Test
    fun scriptMismatchTriesNextLanguage() {
        assertEquals("es", resource(setOf("zh-Hant", "es"), "zh-Hant", "es")
            .getResourceItemByEnvironment(environment("zh-Hans", "es")).path)
    }

    @Test
    fun preservesScriptAndRegionOfSelectedPreference() {
        val resource = resource(setOf("sr-Latn", "sr-Cyrl"), "sr-Latn", "sr-Latn-RS", "sr-Cyrl")
        val env = environment("fr", "sr-Latn-RS", "sr-Cyrl")
        assertEquals("sr-Latn-RS", resource.getResourceItemByEnvironment(env).path)
        assertEquals(ResourceLocale(Locale("sr-Latn-RS")), resource.getResourceLocale(env))
    }

    @Test
    fun secondaryPreferencesAffectEnvironmentEquality() {
        assertNotEquals(environment("fr", "es"), environment("fr", "de"))
        assertNotEquals(environment("fr", "es").hashCode(), environment("fr", "de").hashCode())
        assertEquals(environment("fr", "es"), environment("fr", "es"))
    }

    @Test
    fun legacyResourcesCanUseSecondaryLanguages() {
        val resource = StringResource("legacy", "legacy", setOf(
            ResourceItem(emptySet(), "default", 0, 0),
            ResourceItem(setOf(LanguageQualifier("es")), "es", 0, 0)
        ))
        assertEquals("es", resource.getResourceItemByEnvironment(environment("fr", "es")).path)
    }

    @Test
    fun preferredLocalesAreCopied() {
        val locales = mutableListOf(ResourceLocale(Locale("fr")), ResourceLocale(Locale("es")))
        val env = ResourceEnvironment(locales, ThemeQualifier.LIGHT, DensityQualifier.MDPI)
        locales.clear()
        assertEquals(listOf("fr", "es"), env.locales.map { it.language.language })
    }
}
