/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.DensityQualifier.*
import org.jetbrains.compose.resources.ThemeQualifier.DARK
import org.jetbrains.compose.resources.ThemeQualifier.LIGHT
import kotlin.test.*

@OptIn(ExperimentalResourceApi::class, InternalResourceApi::class)
class ResourceTest {
    @Test
    fun testResourceEquals() = runBlockingTest {
        assertEquals(DrawableResource("a"), DrawableResource("a"))
    }

    @Test
    fun testResourceNotEquals() = runBlockingTest {
        assertNotEquals(DrawableResource("a"), DrawableResource("b"))
    }

    @Test
    fun testGetPathByEnvironment() {
        val resource = DrawableResource(
            id = "ImageResource:test",
            items = setOf(
                ResourceItem(setOf(), "default"),
                ResourceItem(setOf(LanguageQualifier("en")), "en"),
                ResourceItem(setOf(LanguageQualifier("en"), RegionQualifier("US"), XHDPI), "en-rUS-xhdpi"),
                ResourceItem(setOf(LanguageQualifier("fr"), LIGHT), "fr-light"),
                ResourceItem(setOf(DARK), "dark"),
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
            resource.getPathByEnvironment(env("en", "US", DARK, XXHDPI))
        )
        assertEquals(
            "en",
            resource.getPathByEnvironment(env("en", "IN", LIGHT, LDPI))
        )
        assertEquals(
            "default",
            resource.getPathByEnvironment(env("ch", "", LIGHT, MDPI))
        )
        assertEquals(
            "dark",
            resource.getPathByEnvironment(env("ch", "", DARK, MDPI))
        )
        assertEquals(
            "fr-light",
            resource.getPathByEnvironment(env("fr", "", DARK, MDPI))
        )
        assertEquals(
            "fr-light",
            resource.getPathByEnvironment(env("fr", "IN", LIGHT, MDPI))
        )
        assertEquals(
            "default",
            resource.getPathByEnvironment(env("ru", "US", LIGHT, XHDPI))
        )
        assertEquals(
            "dark",
            resource.getPathByEnvironment(env("ru", "US", DARK, XHDPI))
        )

        val resourceWithNoDefault = DrawableResource(
            id = "ImageResource:test2",
            items = setOf(
                ResourceItem(setOf(LanguageQualifier("en")), "en"),
                ResourceItem(setOf(LanguageQualifier("fr"), LIGHT), "fr-light")
            )
        )
        assertFailsWith<IllegalStateException> {
            resourceWithNoDefault.getPathByEnvironment(env("ru", "US", DARK, XHDPI))
        }.message.let { msg ->
            assertEquals("Resource with ID='ImageResource:test2' not found", msg)
        }

        val resourceWithFewFiles = DrawableResource(
            id = "ImageResource:test3",
            items = setOf(
                ResourceItem(setOf(LanguageQualifier("en")), "en1"),
                ResourceItem(setOf(LanguageQualifier("en")), "en2")
            )
        )
        assertFailsWith<IllegalStateException> {
            resourceWithFewFiles.getPathByEnvironment(env("en", "US", DARK, XHDPI))
        }.message.let { msg ->
            assertEquals("Resource with ID='ImageResource:test3' has more than one file: en1, en2", msg)
        }

    }
}
