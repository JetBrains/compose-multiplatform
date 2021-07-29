/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.graphics

import android.os.Build
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidTileModeTest {

    @Test
    fun testTileModeClamp() {
        assertEquals(android.graphics.Shader.TileMode.CLAMP, TileMode.Clamp.toAndroidTileMode())
    }

    @Test
    fun testTileModeRepeat() {
        assertEquals(android.graphics.Shader.TileMode.REPEAT, TileMode.Repeated.toAndroidTileMode())
    }

    @Test
    fun testTileModeMirror() {
        assertEquals(android.graphics.Shader.TileMode.MIRROR, TileMode.Mirror.toAndroidTileMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTileModeDecal() {
        assertEquals(android.graphics.Shader.TileMode.DECAL, TileMode.Decal.toAndroidTileMode())
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.R)
    fun testTileModeDecalCompat() {
        // If we are running on an OS version that is not aware of TileMode.Decal,
        // fallback to clamp
        assertEquals(android.graphics.Shader.TileMode.CLAMP, TileMode.Decal.toAndroidTileMode())
    }

    @Test
    fun testTileModeClampSupported() {
        assertTrue(TileMode.Clamp.isSupported())
    }

    @Test
    fun testTileModeRepeatSupported() {
        assertTrue(TileMode.Repeated.isSupported())
    }

    @Test
    fun testTileModeMirrorSupported() {
        assertTrue(TileMode.Mirror.isSupported())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testTileModeDecalSupported() {
        // TileMode.Decal is supported on Android S and above
        assertTrue(TileMode.Decal.isSupported())
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.R)
    fun testTileModeDecalNotSupported() {
        // TileMode.Decal is not supported on Android R and below
        assertFalse(TileMode.Decal.isSupported())
    }

    @Test
    fun testFrameworkTileModeClampConversion() {
        assertEquals(TileMode.Clamp, android.graphics.Shader.TileMode.CLAMP.toComposeTileMode())
    }

    @Test
    fun testFrameworkTileModeRepeatConversion() {
        assertEquals(TileMode.Repeated, android.graphics.Shader.TileMode.REPEAT.toComposeTileMode())
    }

    @Test
    fun testFrameworkTileModeMirrorConversion() {
        assertEquals(TileMode.Mirror, android.graphics.Shader.TileMode.MIRROR.toComposeTileMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun testFrameworkTileModeDecalConversion() {
        assertEquals(TileMode.Decal, android.graphics.Shader.TileMode.DECAL.toComposeTileMode())
    }
}