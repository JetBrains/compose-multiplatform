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
                ResourceItem(setOf("en"), "en"),
                ResourceItem(setOf("dark"), "dark"),
                ResourceItem(setOf("fr", "light"), "fr-light"),
                ResourceItem(setOf("en", "rUS", "xhdpi"), "en-rUS-xhdpi"),
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
    }
}
