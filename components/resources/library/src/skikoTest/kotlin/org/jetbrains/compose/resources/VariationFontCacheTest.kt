package org.jetbrains.compose.resources

import androidx.compose.ui.text.font.FontVariation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VariationFontCacheTest {

    @Test
    fun `getCacheKey should return an empty string for an empty settings list`() {
        val settings = FontVariation.Settings()
        val cacheKey = settings.getCacheKey()
        assertEquals("", cacheKey, "Cache key for empty settings list should be an empty string")
    }

    @Test
    fun `getCacheKey should return a correct key for a single setting`() {
        val setting = FontVariation.Setting("wght", 700f)
        val settings = FontVariation.Settings(setting)
        val cacheKey = settings.getCacheKey()
        assertEquals("SettingFloat(wght,700.0)", cacheKey, "Cache key for a single setting is incorrect")
    }

    @Test
    fun `getCacheKey should correctly sort settings by class name and axis name`() {
        val setting1 = FontVariation.Setting("wght", 400f)
        val setting2 = FontVariation.Setting("ital", 1f)
        val settings = FontVariation.Settings(setting1, setting2)
        val cacheKey = settings.getCacheKey()
        assertEquals(
            "SettingFloat(ital,1.0),SettingFloat(wght,400.0)",
            cacheKey,
            "Cache key should sort settings by class name and axis name"
        )
    }

    @Test
    fun `getCacheKey should throw an exception when there are duplicate settings`() {
        val setting1 = FontVariation.Setting("wght", 400f)
        val setting2 = FontVariation.Setting("wght", 700f)

        assertFailsWith<IllegalArgumentException>(
            "'axis' must be unique"
        ) {
            FontVariation.Settings(setting1, setting2)
        }
    }
}