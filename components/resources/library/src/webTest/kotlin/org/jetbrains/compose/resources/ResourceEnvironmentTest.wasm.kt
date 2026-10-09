package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.test.ExperimentalTestApi
import org.w3c.dom.events.Event

class ResourceEnvironmentTest {

    // covers https://youtrack.jetbrains.com/issue/CMP-6930 (also see the comments)
    @Test
    fun usingLocaleWithoutRegion() {
        val originalLanguages = getSystemResourceLocales().map { locale ->
            listOf(locale.language.language, locale.script.script, locale.region.region).filter { it.isNotEmpty() }.joinToString("-")
        }
        configureLanguage("en")

        try {
            val env = getSystemEnvironment()
            assertEquals("", env.region.region)
            assertEquals("en", env.language.language)
        } finally {
            configureLanguages(originalLanguages.joinToString(","))
        }
    }

    @Test
    fun usingLocaleWithRegion() {
        val originalLanguages = getSystemResourceLocales().map { locale ->
            listOf(locale.language.language, locale.script.script, locale.region.region).filter { it.isNotEmpty() }.joinToString("-")
        }
        configureLanguage("en-NL")

        try {
            val env = getSystemEnvironment()
            assertEquals("NL", env.region.region)
            assertEquals("en", env.language.language)
        } finally {
            configureLanguages(originalLanguages.joinToString(","))
        }

    }

    @Test
    fun allPreferredLanguagesAreAvailableInOrder() {
        val original = getSystemResourceLocales()
        try {
            configureLanguages("ca-ES,es-ES,de-DE")
            assertEquals(listOf("ca", "es", "de"), getSystemEnvironment().locales.map { it.language.language })
            val resource = StringResource("secondary", "secondary", setOf(
                ResourceItem(emptySet(), "default", 0, 0),
                ResourceItem(setOf(LanguageQualifier("es")), "es", 0, 0)
            ), setOf("es"))
            assertEquals("es", resource.getResourceItemByEnvironment(getSystemEnvironment()).path)
        } finally {
            configureLanguages(original.joinToString(",") { locale ->
                listOf(locale.language.language, locale.script.script, locale.region.region).filter { it.isNotEmpty() }.joinToString("-")
            })
        }
    }


    @Test
    @OptIn(ExperimentalTestApi::class)
    fun languageChangeUpdatesSecondaryPreferences() = clearResourceCachesAndRunUiTest {
        val original = getSystemResourceLocales()
        fun languageTags(locales: List<ResourceLocale>) = locales.joinToString(",") { locale ->
            listOf(locale.language.language, locale.script.script, locale.region.region).filter { it.isNotEmpty() }.joinToString("-")
        }
        try {
            configureLanguages("fr,es")
            var environment: ResourceEnvironment? = null
            setContent { environment = rememberResourceEnvironment() }
            waitResources()
            assertEquals(listOf("fr", "es"), environment!!.locales.map { it.language.language })
            configureLanguages("fr,de")
            window.dispatchEvent(Event("languagechange"))
            waitResources()
            assertEquals(listOf("fr", "de"), environment!!.locales.map { it.language.language })
        } finally {
            configureLanguages(languageTags(original))
            window.dispatchEvent(Event("languagechange"))
        }
    }

}

//language=js
private fun configureLanguage(language: String) = configureLanguages(language)

private fun configureLanguages(languages: String) {
    js("""
       Object.defineProperty(window.navigator, 'languages', {
            get: function () { return languages ? languages.split(',') : []; },
            configurable: true
       });
       Object.defineProperty(window.navigator, 'language', {
            get: function () {
                return languages.split(',')[0];
            },
            configurable: true
        });
    """)
}