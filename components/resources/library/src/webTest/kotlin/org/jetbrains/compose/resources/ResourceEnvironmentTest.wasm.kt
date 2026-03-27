package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceEnvironmentTest {

    // covers https://youtrack.jetbrains.com/issue/CMP-6930 (also see the comments)
    @Test
    fun usingLocaleWithoutRegion() {
        val originalLanguage = window.navigator.language
        configureLanguage("en")

        try {
            val env = getSystemEnvironment()
            assertEquals("", env.region.region)
            assertEquals("en", env.language.language)
        } finally {
            configureLanguage(originalLanguage)
        }
    }

    @Test
    fun usingLocaleWithRegion() {
        val originalLanguage = window.navigator.language
        configureLanguage("en-NL")

        try {
            val env = getSystemEnvironment()
            assertEquals("NL", env.region.region)
            assertEquals("en", env.language.language)
        } finally {
            configureLanguage(originalLanguage)
        }

    }
}

//language=js
private fun configureLanguage(language: String) {
    js("""
       Object.defineProperty(window.navigator, 'language', {
            get: function () {
                return language;
            },
            configurable: true
        });
    """)
}