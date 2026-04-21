/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.DensityQualifier.*
import org.jetbrains.compose.resources.ThemeQualifier.DARK
import org.jetbrains.compose.resources.ThemeQualifier.LIGHT
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
        assertEquals(
            "default",
            resource.getResourceItemByEnvironment(env("de", "", LIGHT, LDPI)).path
        )
        assertEquals(
            "default",
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

        // case 1: language + script + region match (exact)
        assertEquals(
            "sr-Latn-RS",
            resource.getResourceItemByEnvironment(env("sr", "Latn", "RS")).path
        )
        // case 1: language + script match, region falls back
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
        // case 2: language match without script, region hits
        assertEquals(
            "sr-RS",
            resource.getResourceItemByEnvironment(env("sr", "", "RS")).path
        )
        // case 2: language match without script, no region
        assertEquals(
            "sr",
            resource.getResourceItemByEnvironment(env("sr", "", "")).path
        )
        // case 4: no language match -> default
        assertEquals(
            "default",
            resource.getResourceItemByEnvironment(env("en", "", "US")).path
        )

        // case 3: language+region match ignoring script (no language+script and no language-without-script)
        val scriptedOnlyResource = DrawableResource(
            id = "ImageResource:scripted_only",
            items = setOf(
                ResourceItem(setOf(), "default", -1, -1),
                ResourceItem(setOf(LanguageQualifier("sr"), ScriptQualifier("Cyrl"), RegionQualifier("RS")), "sr-Cyrl-RS", -1, -1),
            )
        )
        assertEquals(
            "sr-Cyrl-RS",
            scriptedOnlyResource.getResourceItemByEnvironment(env("sr", "Latn", "RS")).path
        )

        // empty environment script falls through to script-tagged items when no non-script items exist
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
    }
}
