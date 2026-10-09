package org.jetbrains.compose.resources

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.io.encoding.Base64
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 35])
class ResourceEnvironmentAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val originalLocales = if (Build.VERSION.SDK_INT >= 24) LocaleList.getDefault() else null
    private val originalDefaultLocale = Locale.getDefault()

    @After
    fun restoreLocales() {
        if (originalLocales != null) {
            setDefaultLocales(originalLocales, originalLocales.indexOf(originalDefaultLocale))
        } else {
            Locale.setDefault(originalDefaultLocale)
        }
        ResourceCaches.clear()
    }

    @Test
    fun usesSupportedAppLocaleInsteadOfFirstDeviceLanguage() {
        // The device prefers French, but Android selected Spanish for the app (CMP-6840).
        setDefaultLocales(LocaleList(Locale.FRENCH, Locale("es")), 1)
        val context = contextFor("es")
        assertEquals("fr", LocaleList.getDefault()[0].language)
        assertEquals("es", Locale.getDefault().language)
        assertEquals("es", getSystemEnvironment().language.language)
        assertEquals(listOf("es", "fr"), getSystemResourceLocales().map { it.language.language })
        assertMatchesAndroid(context, translatedResource, "cmp6840_translated", "es")
    }

    @Test
    fun preservesConfigurationPreferencesAndSelectsSupportedLanguage() {
        val configuration = Configuration().apply {
            setLocales(LocaleList(Locale.forLanguageTag("ca-ES"), Locale.forLanguageTag("es-ES")))
        }
        var environment: ResourceEnvironment? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                environment = rememberResourceEnvironment()
            }
        }
        composeRule.runOnIdle {
            assertEquals(listOf("ca", "es"), environment!!.locales.map { it.language.language })
            assertEquals("cmp6840/es", translatedResource.getResourceItemByEnvironment(environment!!).path)
        }
    }

    @Test
    fun respectsAppLocaleInsteadOfProcessDefault() {
        LocaleList.setDefault(LocaleList(Locale.FRENCH))
        assertMatchesAndroid(contextFor("es"), translatedResource, "cmp6840_translated", "es")
    }

    @Test
    fun missingTranslationFallsBackToDefaultNotAnotherDeviceLanguage() {
        LocaleList.setDefault(LocaleList(Locale("es"), Locale.FRENCH))
        // A French translation exists, but Android uses default for a missing Spanish entry.
        assertMatchesAndroid(contextFor("es"), untranslatedResource, "cmp6840_untranslated", "default")
    }

    @Test
    @Config(sdk = [23])
    fun usesConfigurationLocaleOnApi23() {
        Locale.setDefault(Locale.FRENCH)
        assertMatchesAndroid(contextFor("es"), translatedResource, "cmp6840_translated", "es")
    }

    @Test
    fun unsupportedAppLocaleFallsBackToDefault() {
        LocaleList.setDefault(LocaleList(Locale.GERMAN))
        assertMatchesAndroid(contextFor("de"), translatedResource, "cmp6840_translated", "default")
    }

    @Test
    fun preservesScriptAndRegionFromConfiguration() {
        LocaleList.setDefault(LocaleList(Locale.FRENCH))
        var environment: ResourceEnvironment? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides contextFor("sr-Latn-RS").resources.configuration) {
                environment = rememberResourceEnvironment()
            }
        }
        composeRule.runOnIdle {
            assertEquals(LanguageQualifier("sr"), environment!!.language)
            assertEquals(ScriptQualifier("Latn"), environment!!.script)
            assertEquals(RegionQualifier("RS"), environment!!.region)
        }
    }

    @Test
    fun pluralRulesUseTheSelectedAppLocale() {
        setDefaultLocales(LocaleList(Locale.FRENCH, Locale("es")), 1)
        val context = contextFor("es")
        val resource = PluralStringResource(
            "cmp6840_plurals", "cmp6840_plurals", setOf(
                ResourceItem(emptySet(), "cmp6840/plurals/default", 0, 0),
                ResourceItem(setOf(LanguageQualifier("es")), "cmp6840/plurals/es", 0, 0)
            )
        )
        var actual = emptyList<String>()
        composeRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides context.resources.configuration,
                LocalResourceReader provides localeReader
            ) {
                actual = (0..2).map { pluralStringResource(resource, it) }
            }
        }
        composeRule.runOnIdle {
            val id = context.resources.getIdentifier("cmp6840_plurals", "plurals", context.packageName)
            assertEquals(listOf("es-other", "es-one", "es-other"), actual)
            assertEquals((0..2).map { context.resources.getQuantityString(id, it) }, actual)
        }
    }

    @Test
    fun configurationChangeUpdatesResourcesWithoutChangingProcessLocale() {
        LocaleList.setDefault(LocaleList(Locale.FRENCH))
        var context by mutableStateOf(contextFor("fr"))
        var actual = ""
        composeRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides context.resources.configuration,
                LocalResourceReader provides localeReader
            ) {
                actual = stringResource(translatedResource)
            }
        }
        composeRule.runOnIdle {
            assertEquals(androidString(context, "cmp6840_translated"), actual)
            assertEquals("default", actual)
            context = contextFor("es")
        }
        composeRule.runOnIdle {
            assertEquals(androidString(context, "cmp6840_translated"), actual)
            assertEquals("es", actual)
            assertEquals("fr", Locale.getDefault().language)
        }
    }

    private fun assertMatchesAndroid(context: Context, resource: StringResource, name: String, expected: String) {
        var actual = ""
        composeRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides context.resources.configuration,
                LocalResourceReader provides localeReader
            ) {
                actual = stringResource(resource)
            }
        }
        composeRule.runOnIdle {
            assertEquals(expected, androidString(context, name))
            assertEquals(expected, actual)
        }
    }

    // Android uses this hidden overload when the selected app locale is not the first device locale.
    private fun setDefaultLocales(locales: LocaleList, selectedIndex: Int) {
        LocaleList::class.java.getDeclaredMethod("setDefault", LocaleList::class.java, Int::class.javaPrimitiveType)
            .invoke(null, locales, selectedIndex)
    }

    private fun contextFor(languageTag: String): Context {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(app.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return app.createConfigurationContext(config)
    }

    private fun androidString(context: Context, name: String): String {
        val id = context.resources.getIdentifier(name, "string", context.packageName)
        check(id != 0) { "Missing Android test resource: $name" }
        return context.resources.getString(id)
    }

    private val translatedResource = stringResource("cmp6840_translated", "es")
    private val untranslatedResource = stringResource("cmp6840_untranslated", "fr")

    private fun stringResource(id: String, language: String) = StringResource(
        id, id, setOf(
            ResourceItem(emptySet(), "cmp6840/default", 0, 0),
            ResourceItem(setOf(LanguageQualifier(language)), "cmp6840/$language", 0, 0)
        )
    )

    private val localeReader = object : ResourceReader {
        override suspend fun read(path: String): ByteArray {
            val language = path.substringAfterLast('/')
            fun encode(value: String) = Base64.encode(value.encodeToByteArray())
            return if ("/plurals/" in path) {
                "plurals|value|one:${encode("$language-one")},other:${encode("$language-other")}".encodeToByteArray()
            } else {
                "string|value|${encode(language)}".encodeToByteArray()
            }
        }

        override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray = read(path)
        override fun getUri(path: String): String = error("Not used")
    }
}
