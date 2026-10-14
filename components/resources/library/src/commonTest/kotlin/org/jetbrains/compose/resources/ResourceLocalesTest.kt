package org.jetbrains.compose.resources

import androidx.compose.ui.text.intl.Locale
import kotlin.test.*

class ResourceLocalesTest {
    private fun environment(vararg tags: String) = ResourceEnvironment(
        tags.map { ResourceLocale(Locale(it)) }, ThemeQualifier.LIGHT, DensityQualifier.MDPI
    )

    private fun resource(vararg translated: String) = StringResource(
        "locale_test", "locale_test",
        setOf(ResourceItem(emptySet(), "default", 0, 0)) + translated.map { tag ->
            val locale = ResourceLocale(Locale(tag))
            ResourceItem(buildSet {
                add(locale.language)
                if (!locale.script.isEmpty()) add(locale.script)
                if (locale.region.region.isNotEmpty()) add(locale.region)
            }, tag, 0, 0)
        }
    )

    @Test
    fun skipsUnsupportedFirstLanguage() {
        assertEquals("es", resource("es").getResourceItemByEnvironment(environment("fr", "es")).path)
    }

    @Test
    fun triesAllPreferencesInOrder() {
        val resource = resource("es", "de")
        assertEquals("es", resource.getResourceItemByEnvironment(environment("ca", "ru", "es", "de")).path)
        assertEquals("de", resource.getResourceItemByEnvironment(environment("ca", "de", "es")).path)
    }

    @Test
    fun incompleteTranslationUsesSecondaryLanguageForEachResource() {
        val env = environment("fr", "de")
        val title = resource("fr", "de")
        val description = resource("de")
        assertEquals("fr", title.getResourceItemByEnvironment(env).path)
        assertEquals("de", description.getResourceItemByEnvironment(env).path)
    }

    @Test
    fun defaultDoesNotCountAsEnglishTranslation() {
        assertEquals("es", resource("es").getResourceItemByEnvironment(environment("en-US", "es")).path)
    }

    @Test
    fun explicitEnglishTranslationParticipatesInPreferenceOrder() {
        assertEquals("en", resource("en", "es").getResourceItemByEnvironment(environment("en-US", "es")).path)
    }

    @Test
    fun noMatchingPreferenceUsesDefault() {
        assertEquals("default", resource("es").getResourceItemByEnvironment(environment("fr", "ca")).path)
    }

    @Test
    fun scriptMismatchTriesNextLanguage() {
        assertEquals("es", resource("zh-Hant", "es")
            .getResourceItemByEnvironment(environment("zh-Hans", "es")).path)
    }

    @Test
    fun regionMismatchTriesNextLanguage() {
        assertEquals("de", resource("fr-CA", "de")
            .getResourceItemByEnvironment(environment("fr-FR", "de")).path)
    }

    @Test
    fun genericPrimaryLanguageWinsOverExactSecondaryLanguage() {
        assertEquals("fr", resource("fr", "de-DE")
            .getResourceItemByEnvironment(environment("fr-FR", "de-DE")).path)
    }

    @Test
    fun preservesScriptAndRegionOfSelectedPreference() {
        val resource = resource("sr-Latn", "sr-Latn-RS", "sr-Cyrl")
        val env = environment("fr", "sr-Latn-RS", "sr-Cyrl")
        assertEquals("sr-Latn-RS", resource.getResourceItemByEnvironment(env).path)
    }

    @Test
    fun appliesThemeAndDensityAfterSelectingSecondaryLanguage() {
        val resource = DrawableResource("localized_image", setOf(
            ResourceItem(emptySet(), "default", 0, 0),
            ResourceItem(setOf(LanguageQualifier("de")), "de", 0, 0),
            ResourceItem(setOf(LanguageQualifier("de"), ThemeQualifier.DARK), "de-dark", 0, 0),
            ResourceItem(setOf(LanguageQualifier("de"), ThemeQualifier.DARK, DensityQualifier.XHDPI), "de-dark-xhdpi", 0, 0)
        ))
        val env = ResourceEnvironment(environment("fr", "de").locales, ThemeQualifier.DARK, DensityQualifier.XHDPI)
        assertEquals("de-dark-xhdpi", resource.getResourceItemByEnvironment(env).path)
    }

    @Test
    fun secondaryPreferencesAffectEnvironmentEquality() {
        assertNotEquals(environment("fr", "es"), environment("fr", "de"))
        assertNotEquals(environment("fr", "es").hashCode(), environment("fr", "de").hashCode())
        assertEquals(environment("fr", "es"), environment("fr", "es"))
    }

    @Test
    fun localeOrderAffectsEnvironmentEquality() {
        assertNotEquals(environment("fr", "de"), environment("de", "fr"))
    }
}
