package org.jetbrains.compose.test.tests.unit

import org.jetbrains.compose.resources.*
import java.nio.file.Paths
import kotlin.test.*

class TestResourceLocalesGeneration {
    // Tests use the published plugin JAR, whose KotlinPoet classes are relocated.
    private fun generate(name: String, vararg args: Any): Any =
        Class.forName("org.jetbrains.compose.resources.GeneratedResClassSpecKt")
            .declaredMethods.single { it.name == name }.invoke(null, *args)

    private fun item(name: String, type: ResourceType, vararg qualifiers: String) = ResourceItem(
        type, qualifiers.toList(), name, Paths.get("${type.typeName}-${qualifiers.joinToString("-")}/$name.xml"), 0
    )

    @Test
    fun collectsLocalesFromAllResourceTypesAndDeduplicates() {
        val items = listOf(
            item("greeting", ResourceType.STRING, "es"),
            item("partial", ResourceType.STRING, "fr"),
            item("font", ResourceType.FONT, "es"),
            item("array", ResourceType.STRING_ARRAY, "de", "rDE"),
            item("quantity", ResourceType.PLURAL_STRING, "ru"),
            item("icon", ResourceType.DRAWABLE, "b+sr+Latn+RS"),
            item("default", ResourceType.STRING)
        )
        val specs = (generate("getAccessorsSpecs", items.groupBy { it.type }.mapValues { (_, list) -> list.groupBy { it.name } },
            "test.resources", "commonMain", "", "Res", false, false) as List<*>).map { it.toString() }
        val locales = specs.single { it.contains("fun _collectCommonMainResourceLocales(") }
        assertTrue(locales.contains("locales.add(\"es\")"), locales)
        assertTrue(locales.contains("locales.add(\"fr\")"), locales)
        assertTrue(locales.contains("locales.add(\"sr-Latn-RS\")"), locales)
        assertTrue(locales.contains("locales.add(\"de-DE\")"), locales)
        assertTrue(locales.contains("locales.add(\"ru\")"), locales)
        assertEquals(1, Regex("locales.add\\(\"es\"\\)").findAll(locales).count())
        specs.filterNot { it.contains("fun _collectCommonMainResourceLocales(") }.forEach {
            assertTrue(it.toString().contains("Res.supportedLocales"), it.toString())
        }
    }

    @Test
    fun mergesMetadataWithoutInitializingIndividualResources() {
        val source = generate("getActualResourceCollectorsFileSpec", "test.resources", "ActualResourceCollectors", "CustomRes", true,
            true, emptyMap<ResourceType, List<String>>(), listOf("_collectDesktopMainResourceLocales", "_collectCommonMainResourceLocales")).toString()
        assertTrue(source.contains("internal actual val CustomRes.supportedLocales: Set<String>"), source)
        assertTrue(source.contains("_collectCommonMainResourceLocales(locales)"), source)
        assertTrue(source.contains("_collectDesktopMainResourceLocales(locales)"), source)
        assertTrue(source.indexOf("_collectCommonMainResourceLocales(locales)") < source.indexOf("_collectDesktopMainResourceLocales(locales)"))
        val initializer = source.substringAfter("supportedLocales").substringBefore("allDrawableResources")
        assertFalse(initializer.contains("Resources(map)"), initializer)
    }

    @Test
    fun metadataIsInternalEvenForPublicRes() {
        val source = generate("getExpectResourceCollectorsFileSpec", "test.resources", "ExpectResourceCollectors", "Res", true).toString()
        assertTrue(source.contains("internal expect val Res.supportedLocales: Set<String>"), source)
    }
}
