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
}
