package org.jetbrains.compose.resources

import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceEnvironmentTest {

    // covers https://youtrack.jetbrains.com/issue/CMP-6930 (also see the comments)
    @Test
    fun usingLocaleWithoutRegion() {
        val originalLanguages = currentLanguages()
        configureLanguages("en")

        try {
            val env = getSystemEnvironment()
            assertEquals("", env.region.region)
            assertEquals("en", env.language.language)
        } finally {
            configureLanguages(originalLanguages)
        }
    }

    @Test
    fun usingLocaleWithRegion() {
        val originalLanguages = currentLanguages()
        configureLanguages("en-NL")

        try {
            val env = getSystemEnvironment()
            assertEquals("NL", env.region.region)
            assertEquals("en", env.language.language)
        } finally {
            configureLanguages(originalLanguages)
        }
    }

    @Test
    fun usingPrioritizedLanguages() {
        val originalLanguages = currentLanguages()
        configureLanguages("fr-CA,es-ES,en")

        try {
            val env = getSystemEnvironment()
            assertEquals(listOf("fr", "es", "en"), env.locales.map { it.language.language })
            assertEquals(listOf("CA", "ES", ""), env.locales.map { it.region.region })
        } finally {
            configureLanguages(originalLanguages)
        }
    }
}

//language=js
private fun currentLanguages(): String = js("window.navigator.languages.join(',')")

// navigator.languages is the preferred-language list; navigator.language is its first entry.
//language=js
private fun configureLanguages(languages: String) {
    js(
        """
       var list = languages.length === 0 ? [] : languages.split(',');
       Object.defineProperty(window.navigator, 'languages', {
            get: function () {
                return list;
            },
            configurable: true
        });
       Object.defineProperty(window.navigator, 'language', {
            get: function () {
                return list.length === 0 ? undefined : list[0];
            },
            configurable: true
        });
    """
    )
}
