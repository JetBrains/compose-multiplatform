/*
 * Copyright 2020 The Android Open Source Project
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

import android.graphics.PorterDuff
import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidBlendModeTest {

    @Test
    fun testBlendModeClearMapsToPorterDuffClear() {
        assertEquals(PorterDuff.Mode.CLEAR, BlendMode.Clear.toPorterDuffMode())
    }

    @Test
    fun testBlendModeSrcMapsToPorterDuffSrc() {
        assertEquals(PorterDuff.Mode.SRC, BlendMode.Src.toPorterDuffMode())
    }

    @Test
    fun testBlendModeDstMapsToPorterDuffDst() {
        assertEquals(PorterDuff.Mode.DST, BlendMode.Dst.toPorterDuffMode())
    }

    @Test
    fun testBlendModeSrcOverMapsToPorterDuffSrcOver() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.SrcOver.toPorterDuffMode())
    }

    @Test
    fun testBlendModeDstOverMapsToPorterDuffDstOver() {
        assertEquals(PorterDuff.Mode.DST_OVER, BlendMode.DstOver.toPorterDuffMode())
    }

    @Test
    fun testBlendModeSrcInMapsToPorterDuffSrcIn() {
        assertEquals(PorterDuff.Mode.SRC_IN, BlendMode.SrcIn.toPorterDuffMode())
    }

    @Test
    fun testBlendModeDstInMapsToPorterDuffDstIn() {
        assertEquals(PorterDuff.Mode.DST_IN, BlendMode.DstIn.toPorterDuffMode())
    }

    @Test
    fun testBlendModeSrcOutMapsToPorterDuffSrcOut() {
        assertEquals(PorterDuff.Mode.SRC_OUT, BlendMode.SrcOut.toPorterDuffMode())
    }

    @Test
    fun testBlendModeDstOutMapsToPorterDuffDstOut() {
        assertEquals(PorterDuff.Mode.DST_OUT, BlendMode.DstOut.toPorterDuffMode())
    }

    @Test
    fun testBlendModeSrcAtopMapsToPorterDuffSrcAtop() {
        assertEquals(PorterDuff.Mode.SRC_ATOP, BlendMode.SrcAtop.toPorterDuffMode())
    }

    @Test
    fun testBlendModeDstAtopMapsToPorterDuffDstAtop() {
        assertEquals(PorterDuff.Mode.DST_ATOP, BlendMode.DstAtop.toPorterDuffMode())
    }

    @Test
    fun testBlendModeXorMapsToPorterDuffXor() {
        assertEquals(PorterDuff.Mode.XOR, BlendMode.Xor.toPorterDuffMode())
    }

    @Test
    fun testBlendModePlusMapsToPorterDuffPlus() {
        assertEquals(PorterDuff.Mode.ADD, BlendMode.Plus.toPorterDuffMode())
    }

    @Test
    fun testBlendModeScreenMapsToPorterDuffScreen() {
        assertEquals(PorterDuff.Mode.SCREEN, BlendMode.Screen.toPorterDuffMode())
    }

    @Test
    fun testBlendModeOverlayMapsToPorterDuffOverlay() {
        assertEquals(PorterDuff.Mode.OVERLAY, BlendMode.Overlay.toPorterDuffMode())
    }

    @Test
    fun testBlendModeDarkenMapsToPorterDuffDarken() {
        assertEquals(PorterDuff.Mode.DARKEN, BlendMode.Darken.toPorterDuffMode())
    }

    @Test
    fun testBlendModeLightenMapsToPorterDuffLighten() {
        assertEquals(PorterDuff.Mode.LIGHTEN, BlendMode.Lighten.toPorterDuffMode())
    }

    @Test
    fun testBlendModeModulateMapsToPorterDuffMultiply() {
        // b/73224934 Android PorterDuff Multiply maps to Skia Modulate
        assertEquals(PorterDuff.Mode.MULTIPLY, BlendMode.Modulate.toPorterDuffMode())
    }

    @Test
    fun testBlendModeColorDodgeMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.ColorDodge.toPorterDuffMode())
    }

    @Test
    fun testBlendModeColorBurnMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.ColorBurn.toPorterDuffMode())
    }

    @Test
    fun testBlendModeHardlightMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.Hardlight.toPorterDuffMode())
    }

    @Test
    fun testBlendModeSoftlightMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.Softlight.toPorterDuffMode())
    }

    @Test
    fun testBlendModeDifferenceMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.Difference.toPorterDuffMode())
    }

    @Test
    fun testBlendModeExclusionMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.Exclusion.toPorterDuffMode())
    }

    @Test
    fun testBlendModeMultiplyMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.Multiply.toPorterDuffMode())
    }

    @Test
    fun testBlendModeHueMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.Hue.toPorterDuffMode())
    }

    @Test
    fun testBlendModeSaturationMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.Saturation.toPorterDuffMode())
    }

    @Test
    fun testBlendModeColorMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.Color.toPorterDuffMode())
    }

    @Test
    fun testBlendModeLuminosityMapsToPorterDuffDefault() {
        assertEquals(PorterDuff.Mode.SRC_OVER, BlendMode.Luminosity.toPorterDuffMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeClearMapsToFramework() {
        assertEquals(android.graphics.BlendMode.CLEAR, BlendMode.Clear.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeSrcMapsToFramework() {
        assertEquals(android.graphics.BlendMode.SRC, BlendMode.Src.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeDstMapsToFramework() {
        assertEquals(android.graphics.BlendMode.DST, BlendMode.Dst.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeSrcOverMapsToFramework() {
        assertEquals(android.graphics.BlendMode.SRC_OVER, BlendMode.SrcOver.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeDstOverMapsToFramework() {
        assertEquals(android.graphics.BlendMode.DST_OVER, BlendMode.DstOver.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeSrcInMapsToFramework() {
        assertEquals(android.graphics.BlendMode.SRC_IN, BlendMode.SrcIn.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeDstInMapsToFramework() {
        assertEquals(android.graphics.BlendMode.DST_IN, BlendMode.DstIn.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeSrcOutMapsToFramework() {
        assertEquals(android.graphics.BlendMode.SRC_OUT, BlendMode.SrcOut.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeDstOutMapsToFramework() {
        assertEquals(android.graphics.BlendMode.DST_OUT, BlendMode.DstOut.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeSrcAtopMapsToFramework() {
        assertEquals(android.graphics.BlendMode.SRC_ATOP, BlendMode.SrcAtop.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeDstAtopMapsToFramework() {
        assertEquals(android.graphics.BlendMode.DST_ATOP, BlendMode.DstAtop.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeXorMapsToFramework() {
        assertEquals(android.graphics.BlendMode.XOR, BlendMode.Xor.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModePlusMapsToFramework() {
        assertEquals(android.graphics.BlendMode.PLUS, BlendMode.Plus.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeScreenMapsToFramework() {
        assertEquals(android.graphics.BlendMode.SCREEN, BlendMode.Screen.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeOverlayMapsToFramework() {
        assertEquals(android.graphics.BlendMode.OVERLAY, BlendMode.Overlay.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeDarkenMapsToFramework() {
        assertEquals(android.graphics.BlendMode.DARKEN, BlendMode.Darken.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeLightenMapsToFramework() {
        assertEquals(android.graphics.BlendMode.LIGHTEN, BlendMode.Lighten.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeModulateMapsToFramework() {
        assertEquals(android.graphics.BlendMode.MODULATE, BlendMode.Modulate.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeColorDodgeMapsToFramework() {
        assertEquals(
            android.graphics.BlendMode.COLOR_DODGE,
            BlendMode.ColorDodge.toAndroidBlendMode()
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeColorBurnMapsToFramework() {
        assertEquals(
            android.graphics.BlendMode.COLOR_BURN,
            BlendMode.ColorBurn.toAndroidBlendMode()
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeHardlightMapsToFramework() {
        assertEquals(
            android.graphics.BlendMode.HARD_LIGHT,
            BlendMode.Hardlight.toAndroidBlendMode()
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeSoftlightMapsToFramework() {
        assertEquals(
            android.graphics.BlendMode.SOFT_LIGHT,
            BlendMode.Softlight.toAndroidBlendMode()
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeDifferenceMapsToFramework() {
        assertEquals(
            android.graphics.BlendMode.DIFFERENCE,
            BlendMode.Difference.toAndroidBlendMode()
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeExclusionMapsToFramework() {
        assertEquals(android.graphics.BlendMode.EXCLUSION, BlendMode.Exclusion.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeMultiplyMapsToFramework() {
        assertEquals(android.graphics.BlendMode.MULTIPLY, BlendMode.Multiply.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeHueMapsToFramework() {
        assertEquals(android.graphics.BlendMode.HUE, BlendMode.Hue.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeSaturationMapsToFramework() {
        assertEquals(
            android.graphics.BlendMode.SATURATION,
            BlendMode.Saturation.toAndroidBlendMode()
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeColorMapsToFramework() {
        assertEquals(android.graphics.BlendMode.COLOR, BlendMode.Color.toAndroidBlendMode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeLuminosityMapsFramework() {
        assertEquals(
            android.graphics.BlendMode.LUMINOSITY,
            BlendMode.Luminosity.toAndroidBlendMode()
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBlendModeToFrameworkCompatibility() {
        // All blend modes should be supported on Android Q and above
        val blendModes = BlendMode.values()
        for (blendMode in blendModes) {
            assertTrue(blendMode.isSupported())
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
    fun testBlendModeToPorterDuffCompatibility() {
        val supportedBlendModes = setOf(
            BlendMode.Clear,
            BlendMode.Src,
            BlendMode.Dst,
            BlendMode.SrcOver,
            BlendMode.DstOver,
            BlendMode.SrcIn,
            BlendMode.DstIn,
            BlendMode.SrcOut,
            BlendMode.DstOut,
            BlendMode.SrcAtop,
            BlendMode.DstAtop,
            BlendMode.Xor,
            BlendMode.Plus,
            BlendMode.Screen,
            BlendMode.Overlay,
            BlendMode.Darken,
            BlendMode.Lighten,
            BlendMode.Modulate
        )
        val blendModes = BlendMode.values()
        for (blendMode in blendModes) {
            if (supportedBlendModes.contains(blendMode)) {
                assertTrue(blendMode.isSupported())
            } else {
                assertFalse(blendMode.isSupported())
            }
        }
    }
}

private fun BlendMode.Companion.values(): Array<BlendMode> =
    arrayOf(
        Clear,
        Src,
        Dst,
        SrcOver,
        DstOver,
        SrcIn,
        DstIn,
        SrcOut,
        DstOut,
        SrcAtop,
        DstAtop,
        Xor,
        Plus,
        Modulate,
        Screen,
        Overlay,
        Darken,
        Lighten,
        ColorDodge,
        ColorBurn,
        Hardlight,
        Softlight,
        Difference,
        Exclusion,
        Multiply,
        Hue,
        Saturation,
        Color,
        Luminosity
    )