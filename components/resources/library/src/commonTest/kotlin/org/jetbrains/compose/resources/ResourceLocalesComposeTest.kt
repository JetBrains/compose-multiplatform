package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.text.intl.Locale
import kotlin.io.encoding.Base64
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ResourceLocalesComposeTest : KmpUiTest() {
    @Test
    fun secondaryLanguageChangesInvalidateCacheAndUpdatePluralRules() = clearResourceCachesAndRunUiTest {
        var languages by mutableStateOf(listOf("fr", "es"))
        val environment = object : ComposeEnvironment {
            @Composable
            override fun rememberEnvironment() = ResourceEnvironment(
                languages.map { ResourceLocale(Locale(it)) }, ThemeQualifier.LIGHT, DensityQualifier.MDPI
            )
        }
        val readPaths = mutableListOf<String>()
        val reader = object : ResourceReader {
            override suspend fun read(path: String): ByteArray {
                readPaths.add(path)
                fun encode(value: String) = Base64.encode(value.encodeToByteArray())
                return "plurals|plural|one:${encode("$path-one")},other:${encode("$path-other")}".encodeToByteArray()
            }
            override suspend fun readPart(path: String, offset: Long, size: Long) = read(path)
            override fun getUri(path: String): String = error("Not used")
        }
        val resource = PluralStringResource("secondary_plural", "secondary_plural", setOf(
            ResourceItem(emptySet(), "default", 0, 0),
            ResourceItem(setOf(LanguageQualifier("es")), "es", 0, 0),
            ResourceItem(setOf(LanguageQualifier("de")), "de", 0, 0)
        ), setOf("es", "de"))
        var text = ""
        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides environment, LocalResourceReader provides reader) {
                text = pluralStringResource(resource, 0)
            }
        }
        waitResources()
        assertEquals("es-other", text) // French uses "one" for zero; the selected Spanish locale uses "other".
        languages = listOf("fr", "de")
        waitResources()
        assertEquals("de-other", text)
        assertEquals(listOf("es", "de"), readPaths)
    }
}
