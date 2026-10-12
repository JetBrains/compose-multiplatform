/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.DensityQualifier.*
import org.jetbrains.compose.resources.ThemeQualifier.DARK
import org.jetbrains.compose.resources.ThemeQualifier.LIGHT
import androidx.compose.ui.text.intl.Locale
import kotlin.test.*

class ResourceTest {
    @Test
    fun testResourceEquals() {
        assertEquals(TestDrawableResource("a"), TestDrawableResource("a"))
    }

    @Test
    fun testResourceNotEquals() {
        assertNotEquals(TestDrawableResource("a"), TestDrawableResource("b"))
    }

    @Test
    fun testGetPathByEnvironment() {
        val resource = DrawableResource(
            id = "ImageResource:test",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en")), "en", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en"), RegionQualifier("US"), XHDPI), "en-rUS-xhdpi", -1, -1),
                ResourceItem(setOf(LanguageQualifier("de"), RegionQualifier("US")), "de-rUS", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr"), LIGHT), "fr-light", -1, -1),
                ResourceItem(setOf(DARK), "dark", -1, -1),
            )
        )
        fun env(lang: String, reg: String, theme: ThemeQualifier, density: DensityQualifier) = ResourceEnvironment(
            language = LanguageQualifier(lang),
            script = ScriptQualifier(""),
            region = RegionQualifier(reg),
            theme = theme,
            density = density
        )
        assertEquals(
            "en-rUS-xhdpi",
            resource.getResourceItemByEnvironment(env("en", "US", DARK, XXHDPI)).path
        )
        assertEquals(
            "en",
            resource.getResourceItemByEnvironment(env("en", "IN", LIGHT, LDPI)).path
        )
        assertEquals(
            "de-rUS",
            resource.getResourceItemByEnvironment(env("de", "US", LIGHT, LDPI)).path
        )
        // de and de-IN both fall back to the only German folder, like Android's parent/child
        // locale matching: https://developer.android.com/guide/topics/resources/multilingual-support
        assertEquals(
            "de-rUS",
            resource.getResourceItemByEnvironment(env("de", "", LIGHT, LDPI)).path
        )
        assertEquals(
            "de-rUS",
            resource.getResourceItemByEnvironment(env("de", "IN", LIGHT, LDPI)).path
        )
        assertEquals(
            "default",
            resource.getResourceItemByEnvironment(env("ch", "", LIGHT, MDPI)).path
        )
        assertEquals(
            "dark",
            resource.getResourceItemByEnvironment(env("ch", "", DARK, MDPI)).path
        )
        assertEquals(
            "fr-light",
            resource.getResourceItemByEnvironment(env("fr", "", DARK, MDPI)).path
        )
        assertEquals(
            "fr-light",
            resource.getResourceItemByEnvironment(env("fr", "IN", LIGHT, MDPI)).path
        )
        assertEquals(
            "default",
            resource.getResourceItemByEnvironment(env("ru", "US", LIGHT, XHDPI)).path
        )
        assertEquals(
            "dark",
            resource.getResourceItemByEnvironment(env("ru", "US", DARK, XHDPI)).path
        )

        val resourceWithNoDefault = DrawableResource(
            id = "ImageResource:test2",
            items = setOf(
                ResourceItem(setOf(LanguageQualifier("en")), "en", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr"), LIGHT), "fr-light", -1, -1)
            )
        )
        assertFailsWith<IllegalStateException> {
            resourceWithNoDefault.getResourceItemByEnvironment(env("ru", "US", DARK, XHDPI))
        }.message.let { msg ->
            assertEquals("Resource with ID='ImageResource:test2' not found", msg)
        }

        val resourceWithFewFiles = DrawableResource(
            id = "ImageResource:test3",
            items = setOf(
                ResourceItem(setOf(LanguageQualifier("en")), "en1", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en")), "en2", -1, -1)
            )
        )
        assertFailsWith<IllegalStateException> {
            resourceWithFewFiles.getResourceItemByEnvironment(env("en", "US", DARK, XHDPI))
        }.message.let { msg ->
            assertEquals("Resource with ID='ImageResource:test3' has more than one file: en1, en2", msg)
        }
    }

    @Test
    fun testGetPathByEnvironmentWithScript() {
        val resource = DrawableResource(
            id = "ImageResource:script_test",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("sr")), "sr", -1, -1),
                ResourceItem(setOf(LanguageQualifier("sr"), ScriptQualifier("Latn")), "sr-Latn", -1, -1),
                ResourceItem(setOf(LanguageQualifier("sr"), ScriptQualifier("Cyrl")), "sr-Cyrl", -1, -1),
                ResourceItem(setOf(LanguageQualifier("sr"), ScriptQualifier("Latn"), RegionQualifier("RS")), "sr-Latn-RS", -1, -1),
                ResourceItem(setOf(LanguageQualifier("sr"), RegionQualifier("RS")), "sr-RS", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), ScriptQualifier("Hans")), "zh-Hans", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), ScriptQualifier("Hant")), "zh-Hant", -1, -1),
            )
        )
        fun env(lang: String, script: String, reg: String) = ResourceEnvironment(
            language = LanguageQualifier(lang),
            script = ScriptQualifier(script),
            region = RegionQualifier(reg),
            theme = LIGHT,
            density = XHDPI
        )

        // case 1: language + script match (narrowed by region if possible)
        assertEquals(
            "sr-Latn-RS",
            resource.getResourceItemByEnvironment(env("sr", "Latn", "RS")).path
        )
        assertEquals(
            "sr-Latn",
            resource.getResourceItemByEnvironment(env("sr", "Latn", "")).path
        )
        assertEquals(
            "sr-Latn",
            resource.getResourceItemByEnvironment(env("sr", "Latn", "BA")).path
        )
        assertEquals(
            "sr-Cyrl",
            resource.getResourceItemByEnvironment(env("sr", "Cyrl", "")).path
        )
        assertEquals(
            "zh-Hans",
            resource.getResourceItemByEnvironment(env("zh", "Hans", "")).path
        )
        assertEquals(
            "zh-Hant",
            resource.getResourceItemByEnvironment(env("zh", "Hant", "")).path
        )

        // case 2: language match without script (narrowed by region if possible)
        assertEquals(
            "sr-RS",
            resource.getResourceItemByEnvironment(env("sr", "", "RS")).path
        )
        assertEquals(
            "sr",
            resource.getResourceItemByEnvironment(env("sr", "", "")).path
        )

        // case 3: env-requested script must not cross to a different one (Hans vs Hant)
        val scriptedOnlyResource = DrawableResource(
            id = "ImageResource:scripted_only",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("sr"), ScriptQualifier("Cyrl"), RegionQualifier("RS")), "sr-Cyrl-RS", -1, -1),
            )
        )
        assertEquals(
            "default",
            scriptedOnlyResource.getResourceItemByEnvironment(env("sr", "Latn", "RS")).path
        )

        val zhSimplifiedTraditional = DrawableResource(
            id = "ImageResource:zh_hans_hant",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), ScriptQualifier("Hans"), RegionQualifier("CN")), "zh-Hans-CN", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), ScriptQualifier("Hant"), RegionQualifier("TW")), "zh-Hant-TW", -1, -1),
            )
        )
        assertEquals(
            "default",
            zhSimplifiedTraditional.getResourceItemByEnvironment(env("zh", "Hans", "TW")).path
        )

        // case 3 (variant): empty environment script does fall back across scripts
        val scriptOnlyResource = DrawableResource(
            id = "ImageResource:script_only",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("sr"), ScriptQualifier("Latn")), "sr-Latn", -1, -1),
            )
        )
        assertEquals(
            "sr-Latn",
            scriptOnlyResource.getResourceItemByEnvironment(env("sr", "", "")).path
        )

        // case 4: no language match -> default
        assertEquals(
            "default",
            resource.getResourceItemByEnvironment(env("en", "", "US")).path
        )
    }

    @Test
    fun testPrioritizedLocalesExactRegionThenLanguageThenDefault() {
        val resource = DrawableResource(
            id = "ImageResource:multilocale_test",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en")), "en", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en"), RegionQualifier("US")), "en-US", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr"), RegionQualifier("FR")), "fr-FR", -1, -1),
                ResourceItem(setOf(LanguageQualifier("es")), "es", -1, -1),
                ResourceItem(setOf(LanguageQualifier("es"), RegionQualifier("ES")), "es-ES", -1, -1),
                ResourceItem(setOf(LanguageQualifier("de")), "de", -1, -1),
            )
        )

        assertEquals(
            "fr-FR",
            resource.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("fr", "", "FR"),
                    LocaleQualifiers("es", "", "ES"),
                    LocaleQualifiers("en", "", "US")
                )
            ).path
        )

        // Another region of the same language is Android parent/child locale matching.
        assertEquals(
            "fr-FR",
            resource.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("fr", "", "CA"),
                    LocaleQualifiers("es", "", "MX"),
                    LocaleQualifiers("en", "", "US")
                )
            ).path
        )

        assertEquals(
            "de",
            resource.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("it", "", "IT"),
                    LocaleQualifiers("pt", "", "BR"),
                    LocaleQualifiers("de", "", "DE")
                )
            ).path
        )

        assertEquals(
            "default",
            resource.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("ja", "", "JP"),
                    LocaleQualifiers("ko", "", "KR"),
                    LocaleQualifiers("ru", "", "RU")
                )
            ).path
        )
    }

    @Test
    fun testLanguageWithoutRegionBeatsLaterLocale() {
        val resourceWithBaseFr = DrawableResource(
            id = "ImageResource:base_fr_test",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr")), "fr", -1, -1),
                ResourceItem(setOf(LanguageQualifier("es"), RegionQualifier("ES")), "es-ES", -1, -1)
            )
        )
        assertEquals(
            "fr",
            resourceWithBaseFr.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("fr", "", "CA"),
                    LocaleQualifiers("es", "", "ES")
                )
            ).path
        )

        val resourceWithEnAndEs = DrawableResource(
            id = "ImageResource:en_es_test",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en")), "en", -1, -1),
                ResourceItem(setOf(LanguageQualifier("es")), "es", -1, -1),
            )
        )
        assertEquals(
            "en",
            resourceWithEnAndEs.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("en", "", "IN"),
                    LocaleQualifiers("es", "", "US")
                )
            ).path
        )
    }

    @Test
    fun testUnqualifiedValuesIsLastResortNotEnglish() {
        val resourceDefaultAndEs = DrawableResource(
            id = "ImageResource:default_es_test",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("es")), "es", -1, -1),
            )
        )
        assertEquals(
            "es",
            resourceDefaultAndEs.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("en", "", "IN"),
                    LocaleQualifiers("es", "", "US")
                )
            ).path
        )
    }

    @Test
    fun testScriptMismatchAdvancesToNextPreferredLocale() {
        val resourceWithScript = DrawableResource(
            id = "ImageResource:script_fallback_test",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), ScriptQualifier("Hant")), "zh-Hant", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en")), "en", -1, -1)
            )
        )
        assertEquals(
            "en",
            resourceWithScript.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("zh", "Hans", "CN"),
                    LocaleQualifiers("en", "", "US")
                )
            ).path
        )
    }

    @Test
    fun testChineseNeverCrossesScripts() {
        val resourceZhRegionOnly = DrawableResource(
            id = "ImageResource:zh_region_test",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), RegionQualifier("CN")), "zh-rCN", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), RegionQualifier("TW")), "zh-rTW", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en")), "en", -1, -1),
            )
        )
        // A requested script blocks the other-region fallback, so Hant/Hans is never guessed
        // from a region-only folder; advance to English instead.
        assertEquals(
            "en",
            resourceZhRegionOnly.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("zh", "Hant", "US"),
                    LocaleQualifiers("en", "", "US")
                )
            ).path
        )
        assertEquals(
            "en",
            resourceZhRegionOnly.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("zh", "Hans", ""),
                    LocaleQualifiers("en", "", "US")
                )
            ).path
        )
        assertEquals(
            "zh-rTW",
            resourceZhRegionOnly.getResourceItemByEnvironment(
                multiLocaleEnv(LocaleQualifiers("zh", "Hant", "TW"))
            ).path
        )

        val singleTaiwanResource = DrawableResource(
            id = "ImageResource:single_tw_test",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), RegionQualifier("TW")), "zh-rTW", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en")), "en", -1, -1),
            )
        )
        // Explicit Hans must not fall back to TW even when TW is the only Chinese region folder
        assertEquals(
            "en",
            singleTaiwanResource.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("zh", "Hans", "CN"),
                    LocaleQualifiers("en", "", "US")
                )
            ).path
        )
    }

    @Test
    fun testRegionFallbackStaysWithinOneScript() {
        // https://github.com/JetBrains/compose-multiplatform/issues/4571
        val traditionalOnly = DrawableResource(
            id = "ImageResource:zh_traditional_regions",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), RegionQualifier("HK")), "zh-rHK", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), RegionQualifier("TW")), "zh-rTW", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), RegionQualifier("MO")), "zh-rMO", -1, -1),
            )
        )
        // zh-CN implies Hans, so none of the Hant folders may be used
        assertEquals(
            "default",
            traditionalOnly.getResourceItemByEnvironment(
                multiLocaleEnv(LocaleQualifiers("zh", "", "CN"))
            ).path
        )
        // zh-HK implies Hant, so a sibling Hant region is fine
        assertEquals(
            "zh-rHK",
            traditionalOnly.getResourceItemByEnvironment(
                multiLocaleEnv(LocaleQualifiers("zh", "", "SG"), LocaleQualifiers("zh", "", "HK"))
            ).path
        )

        val simplifiedOnly = DrawableResource(
            id = "ImageResource:zh_simplified_region",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), RegionQualifier("CN")), "zh-rCN", -1, -1),
            )
        )
        // zh-SG implies Hans like zh-CN, so the fallback applies
        assertEquals(
            "zh-rCN",
            simplifiedOnly.getResourceItemByEnvironment(
                multiLocaleEnv(LocaleQualifiers("zh", "", "SG"))
            ).path
        )
        assertEquals(
            "default",
            simplifiedOnly.getResourceItemByEnvironment(
                multiLocaleEnv(LocaleQualifiers("zh", "", "TW"))
            ).path
        )
    }

    @Test
    fun testChineseScriptQualifiedFoldersIsolation() {
        val scriptOnlyChinese = DrawableResource(
            id = "ImageResource:zh_script_qualified",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), ScriptQualifier("Hans")), "zh-Hans", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), ScriptQualifier("Hant")), "zh-Hant", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en")), "en", -1, -1),
            )
        )

        // zh-CN (no script in tag) implies Hans, so it must match zh-Hans
        assertEquals(
            "zh-Hans",
            scriptOnlyChinese.getResourceItemByEnvironment(
                multiLocaleEnv(LocaleQualifiers("zh", "", "CN"))
            ).path
        )

        // zh-TW (no script in tag) implies Hant, so it must match zh-Hant
        assertEquals(
            "zh-Hant",
            scriptOnlyChinese.getResourceItemByEnvironment(
                multiLocaleEnv(LocaleQualifiers("zh", "", "TW"))
            ).path
        )

        val traditionalScriptOnly = DrawableResource(
            id = "ImageResource:zh_traditional_script_only",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("zh"), ScriptQualifier("Hant")), "zh-Hant", -1, -1),
                ResourceItem(setOf(LanguageQualifier("en")), "en", -1, -1),
            )
        )

        // zh-CN implies Hans, so it must NOT substitute zh-Hant; it should advance to English
        assertEquals(
            "en",
            traditionalScriptOnly.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("zh", "", "CN"),
                    LocaleQualifiers("en", "", "US")
                )
            ).path
        )

        // zh-TW implies Hant, so it matches zh-Hant
        assertEquals(
            "zh-Hant",
            traditionalScriptOnly.getResourceItemByEnvironment(
                multiLocaleEnv(LocaleQualifiers("zh", "", "TW"))
            ).path
        )
    }

    @Test
    fun testOtherRegionOfTheSameLanguageWins() {
        val resourceRegionsOnly = DrawableResource(
            id = "ImageResource:regions_only",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr"), RegionQualifier("FR")), "fr-FR", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr"), RegionQualifier("BE")), "fr-BE", -1, -1),
                ResourceItem(setOf(LanguageQualifier("es")), "es", -1, -1),
            )
        )
        assertEquals(
            "fr-BE",
            resourceRegionsOnly.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("fr", "", "BE"),
                    LocaleQualifiers("es", "", "US")
                )
            ).path
        )
        // Several regions of the same language: the lowest region code wins, and French still
        // beats Spanish because French is the more preferred language.
        assertEquals(
            "fr-BE",
            resourceRegionsOnly.getResourceItemByEnvironment(
                multiLocaleEnv(
                    LocaleQualifiers("fr", "", "CA"),
                    LocaleQualifiers("es", "", "US")
                )
            ).path
        )
        assertEquals(
            "fr-BE",
            resourceRegionsOnly.getResourceItemByEnvironment(
                multiLocaleEnv(LocaleQualifiers("fr", "", "CA"))
            ).path
        )

        // A region-less folder is preferred over any of them, so the tie-break is only
        // reached by a resource set that has none
        val resourceWithBaseFr = DrawableResource(
            id = "ImageResource:regions_and_base",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr")), "fr", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr"), RegionQualifier("FR")), "fr-FR", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr"), RegionQualifier("BE")), "fr-BE", -1, -1),
            )
        )
        assertEquals(
            "fr",
            resourceWithBaseFr.getResourceItemByEnvironment(
                multiLocaleEnv(LocaleQualifiers("fr", "", "CA"))
            ).path
        )

        // Several densities under one fallback region must not break locale matching
        val multiDensity = DrawableResource(
            id = "ImageResource:multidensity",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr"), RegionQualifier("FR"), MDPI), "fr-FR-mdpi", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr"), RegionQualifier("FR"), HDPI), "fr-FR-hdpi", -1, -1),
                ResourceItem(setOf(LanguageQualifier("fr"), RegionQualifier("FR"), XHDPI), "fr-FR-xhdpi", -1, -1),
            )
        )
        assertEquals(
            "fr-FR-hdpi",
            multiDensity.getResourceItemByEnvironment(
                ResourceEnvironment(
                    locales = listOf(LocaleQualifiers("fr", "", "CA")),
                    theme = LIGHT,
                    density = HDPI
                )
            ).path
        )
    }

    @Test
    fun testSelectResourceLocalesAppendsOsListBehindCompose() {
        val compose = listOf(LocaleQualifiers("en", "", "US"))
        val system = listOf(
            LocaleQualifiers("en", "", "US"),
            LocaleQualifiers("zh", "Hans", "CN"),
        )
        // The platform default is already at the head of the OS list: nothing to move
        assertEquals(system, selectResourceLocales(compose, system))
    }

    @Test
    fun testSelectResourceLocalesKeepsOverrideThatOsListAlsoContains() {
        // An app that calls Locale.setDefault(GERMAN) must get German, even though the OS
        // lists German behind French
        val compose = listOf(LocaleQualifiers("de", "", "DE"))
        val system = listOf(
            LocaleQualifiers("fr", "", "FR"),
            LocaleQualifiers("en", "", "US"),
            LocaleQualifiers("de", "", "DE"),
        )
        assertEquals(
            listOf(
                LocaleQualifiers("de", "", "DE"),
                LocaleQualifiers("fr", "", "FR"),
                LocaleQualifiers("en", "", "US"),
            ),
            selectResourceLocales(compose, system)
        )
    }

    @Test
    fun testSelectResourceLocalesKeepsComposeWhenOsListHasOne() {
        val compose = listOf(LocaleQualifiers("de", "", "DE"))
        val system = listOf(LocaleQualifiers("fr", "", "FR"))
        assertEquals(compose, selectResourceLocales(compose, system))
    }

    @Test
    fun testSelectResourceLocalesKeepsComposeWhenItAlreadyHasFallbacks() {
        val compose = listOf(
            LocaleQualifiers("de", "", "DE"),
            LocaleQualifiers("en", "", "US"),
        )
        val system = listOf(
            LocaleQualifiers("fr", "", "FR"),
            LocaleQualifiers("es", "", "ES"),
        )
        assertEquals(compose, selectResourceLocales(compose, system))
    }

    @Test
    fun testSelectResourceLocalesPrependsComposeOverride() {
        val compose = listOf(LocaleQualifiers("de", "", "DE"))
        val system = listOf(
            LocaleQualifiers("fr", "", "FR"),
            LocaleQualifiers("es", "", "ES"),
        )
        assertEquals(listOf(compose.single()) + system, selectResourceLocales(compose, system))
    }

    @Test
    fun testSelectResourceLocalesPrependsComposeRegionOverride() {
        val compose = listOf(LocaleQualifiers("es", "", "MX"))
        val system = listOf(
            LocaleQualifiers("es", "", "ES"),
            LocaleQualifiers("en", "", "US"),
        )
        assertEquals(listOf(compose.single()) + system, selectResourceLocales(compose, system))
    }

    @Test
    fun testSelectResourceLocalesUsesSystemWhenComposeIsEmpty() {
        val system = listOf(
            LocaleQualifiers("hi", "", "IN"),
            LocaleQualifiers("en", "", "US"),
        )
        assertEquals(system, selectResourceLocales(emptyList(), system))
    }

    private fun multiLocaleEnv(vararg locales: LocaleQualifiers) = ResourceEnvironment(
        locales = locales.toList(),
        theme = LIGHT,
        density = MDPI
    )
}
