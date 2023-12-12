/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.DensityQualifier.*
import org.jetbrains.compose.resources.ThemeQualifier.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

@OptIn(ExperimentalResourceApi::class)
class ResourceTest {
    @Test
    fun testResourceEquals() = runBlockingTest {
        assertEquals(ImageResource("a"), ImageResource("a"))
    }

    @Test
    fun testResourceNotEquals() = runBlockingTest {
        assertNotEquals(ImageResource("a"), ImageResource("b"))
    }

    @Test
    fun testMissingResource() = runBlockingTest {
        assertFailsWith<MissingResourceException> {
            readResourceBytes("missing.png")
        }
        val error = assertFailsWith<IllegalStateException> {
            getString(TestStringResource("unknown_id"))
        }
        assertEquals("String ID=`unknown_id` is not found!", error.message)
    }

    @Test
    fun testReadFileResource() = runBlockingTest {
        val bytes = readResourceBytes("strings.xml")
        assertEquals(
            """
                <resources>
                    <string name="app_name">Compose Resources App</string>
                    <string name="hello">😊 Hello world!</string>
                    <string name="str_template">Hello, %1${'$'}s! You have %2${'$'}d new messages.</string>
                    <string-array name="str_arr">
                        <item>item 1</item>
                        <item>item 2</item>
                        <item>item 3</item>
                    </string-array>
                </resources>
                
            """.trimIndent(),
            bytes.decodeToString()
        )
    }

    @Test
    fun testLoadStringResource() = runBlockingTest {
        assertEquals("Compose Resources App", getString(TestStringResource("app_name")))
        assertEquals(
            "Hello, test-name! You have 42 new messages.",
            getString(TestStringResource("str_template"), "test-name", 42)
        )
        assertEquals(listOf("item 1", "item 2", "item 3"), getStringArray(TestStringResource("str_arr")))
    }
    @Test
    fun testGetPathByEnvironment() {
        val resource = ImageResource(
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
