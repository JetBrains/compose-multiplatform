package org.jetbrains.compose.resources

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

private var osLocales: List<LocaleQualifiers> = emptyList()

private fun osPreferredLocales(): List<LocaleQualifiers> = osLocales

@OptIn(ExperimentalTestApi::class)
class DefaultComposeEnvironmentTest {
    private val originalLocale: Locale = Locale.getDefault()
    private val originalPreferredLocales = getPreferredLocales

    @AfterTest
    fun restore() {
        Locale.setDefault(originalLocale)
        getPreferredLocales = originalPreferredLocales
    }

    private fun composedLocalesFor(
        jvmDefault: Locale,
        vararg osList: LocaleQualifiers
    ): List<LocaleQualifiers> {
        Locale.setDefault(jvmDefault)
        osLocales = osList.toList()
        getPreferredLocales = ::osPreferredLocales
        var locales: List<LocaleQualifiers>? = null
        runComposeUiTest {
            setContent { locales = rememberResourceEnvironment().locales }
            waitForIdle()
        }
        return checkNotNull(locales)
    }

    @Test
    fun theJvmDefaultLocaleStaysInFrontOfTheOsList() {
        assertEquals(
            listOf(
                LocaleQualifiers("de", "", "DE"),
                LocaleQualifiers("fr", "", "FR"),
                LocaleQualifiers("en", "", "US"),
            ),
            composedLocalesFor(
                Locale.GERMANY,
                LocaleQualifiers("fr", "", "FR"),
                LocaleQualifiers("en", "", "US"),
                LocaleQualifiers("de", "", "DE"),
            )
        )
    }

    @Test
    fun theOsListSuppliesTheFallbackChain() {
        assertEquals(
            listOf(
                LocaleQualifiers("fr", "", "FR"),
                LocaleQualifiers("es", "", "ES"),
            ),
            composedLocalesFor(
                Locale.FRANCE,
                LocaleQualifiers("fr", "", "FR"),
                LocaleQualifiers("es", "", "ES"),
            )
        )
    }
}
