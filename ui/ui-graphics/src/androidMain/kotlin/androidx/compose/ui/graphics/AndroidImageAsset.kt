/*
 * Copyright 2019 The Android Open Source Project
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

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.UnsupportedOperationException

/**
 * Create an [ImageAsset] from an image file stored in resources for the application
 *
 * @param res Resources object to query the image file from
 * @param resId Identifier for the image asset to query from [res]
 *
 * @return Loaded image file represented as an [ImageAsset]
 */
fun imageFromResource(res: Resources, resId: Int): ImageAsset {
    return AndroidImageAsset(BitmapFactory.decodeResource(res, resId))
}

/**
 * Create an [ImageAsset] from the given [Bitmap]. Note this does
 * not create a copy of the original [Bitmap] and changes to it
 * will modify the returned [ImageAsset]
 */
fun Bitmap.asImageAsset(): ImageAsset = AndroidImageAsset(this)

internal actual fun ActualImageAsset(
    width: Int,
    height: Int,
    config: ImageAssetConfig,
    hasAlpha: Boolean,
    colorSpace: ColorSpace
): ImageAsset {
    val bitmapConfig = config.toBitmapConfig()
    val bitmap: Bitmap
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Note intentionally ignoring density in all cases
        bitmap = Bitmap.createBitmap(
            null,
            width,
            height,
            bitmapConfig,
            hasAlpha,
            colorSpace.toFrameworkColorSpace()
        )
    } else {
        bitmap = Bitmap.createBitmap(
            null as DisplayMetrics?,
            width,
            height,
            bitmapConfig
        )
        bitmap.setHasAlpha(hasAlpha)
    }
    return AndroidImageAsset(bitmap)
}

/**
 * @Throws UnsupportedOperationException if this [ImageAsset] is not backed by an
 * android.graphics.Bitmap
 */
fun ImageAsset.asAndroidBitmap(): Bitmap =
    when (this) {
        is AndroidImageAsset -> bitmap
        else -> throw UnsupportedOperationException("Unable to obtain android.graphics.Bitmap")
    }

internal class AndroidImageAsset(internal val bitmap: Bitmap) : ImageAsset {

    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height

    override val config: ImageAssetConfig
        get() = bitmap.config.toImageConfig()

    override val colorSpace: ColorSpace
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bitmap.colorSpace?.toComposeColorSpace() ?: ColorSpaces.Srgb
        } else {
            ColorSpaces.Srgb
        }

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int
    ) {
        // Internal Android implementation that copies the pixels from the underlying
        // android.graphics.Bitmap if the configuration supports it
        val androidBitmap = asAndroidBitmap()
        var recycleTarget = false
        val targetBitmap =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                androidBitmap.config != Bitmap.Config.HARDWARE
            ) {
                androidBitmap
            } else {
                // Because we are creating a copy for the purposes of reading pixels out of it
                // be sure to recycle this temporary bitmap when we are finished with it.
                recycleTarget = true

                // Pixels of a hardware bitmap cannot be queried directly so make a copy
                // of it into a configuration that can be queried
                // Passing in false for the isMutable parameter as we only intend to read pixel
                // information from the bitmap
                androidBitmap.copy(Bitmap.Config.ARGB_8888, false)
            }

        targetBitmap.getPixels(
            buffer,
            bufferOffset,
            stride,
            startX,
            startY,
            width,
            height
        )
        // Recycle the target if we are done with it
        if (recycleTarget) {
            targetBitmap.recycle()
        }
    }

    override val hasAlpha: Boolean
        get() = bitmap.hasAlpha()

    override fun prepareToDraw() {
        bitmap.prepareToDraw()
    }
}

internal fun ImageAssetConfig.toBitmapConfig(): Bitmap.Config {
    // Cannot utilize when statements with enums that may have different sets of supported
    // values between the compiled SDK and the platform version of the device.
    // As a workaround use if/else statements
    // See https://youtrack.jetbrains.com/issue/KT-30473 for details
    return if (this == ImageAssetConfig.Argb8888) {
        Bitmap.Config.ARGB_8888
    } else if (this == ImageAssetConfig.Alpha8) {
        Bitmap.Config.ALPHA_8
    } else if (this == ImageAssetConfig.Rgb565) {
        Bitmap.Config.RGB_565
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == ImageAssetConfig.F16) {
        Bitmap.Config.RGBA_F16
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == ImageAssetConfig.Gpu) {
        Bitmap.Config.HARDWARE
    } else {
        Bitmap.Config.ARGB_8888
    }
}

internal fun Bitmap.Config.toImageConfig(): ImageAssetConfig {
    // Cannot utilize when statements with enums that may have different sets of supported
    // values between the compiled SDK and the platform version of the device.
    // As a workaround use if/else statements
    // See https://youtrack.jetbrains.com/issue/KT-30473 for details
    @Suppress("DEPRECATION")
    return if (this == Bitmap.Config.ALPHA_8) {
        ImageAssetConfig.Alpha8
    } else if (this == Bitmap.Config.RGB_565) {
        ImageAssetConfig.Rgb565
    } else if (this == Bitmap.Config.ARGB_4444) {
        ImageAssetConfig.Argb8888 // Always upgrade to Argb_8888
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == Bitmap.Config.RGBA_F16) {
        ImageAssetConfig.F16
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == Bitmap.Config.HARDWARE) {
        ImageAssetConfig.Gpu
    } else {
        ImageAssetConfig.Argb8888
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ColorSpace.toFrameworkColorSpace(): android.graphics.ColorSpace {
    val frameworkNamedSpace = when (this) {
        ColorSpaces.Srgb -> android.graphics.ColorSpace.Named.SRGB
        ColorSpaces.Aces -> android.graphics.ColorSpace.Named.ACES
        ColorSpaces.Acescg -> android.graphics.ColorSpace.Named.ACESCG
        ColorSpaces.AdobeRgb -> android.graphics.ColorSpace.Named.ADOBE_RGB
        ColorSpaces.Bt2020 -> android.graphics.ColorSpace.Named.BT2020
        ColorSpaces.Bt709 -> android.graphics.ColorSpace.Named.BT709
        ColorSpaces.CieLab -> android.graphics.ColorSpace.Named.CIE_LAB
        ColorSpaces.CieXyz -> android.graphics.ColorSpace.Named.CIE_XYZ
        ColorSpaces.DciP3 -> android.graphics.ColorSpace.Named.DCI_P3
        ColorSpaces.DisplayP3 -> android.graphics.ColorSpace.Named.DISPLAY_P3
        ColorSpaces.ExtendedSrgb -> android.graphics.ColorSpace.Named.EXTENDED_SRGB
        ColorSpaces.LinearExtendedSrgb ->
            android.graphics.ColorSpace.Named.LINEAR_EXTENDED_SRGB
        ColorSpaces.LinearSrgb -> android.graphics.ColorSpace.Named.LINEAR_SRGB
        ColorSpaces.Ntsc1953 -> android.graphics.ColorSpace.Named.NTSC_1953
        ColorSpaces.ProPhotoRgb -> android.graphics.ColorSpace.Named.PRO_PHOTO_RGB
        ColorSpaces.SmpteC -> android.graphics.ColorSpace.Named.SMPTE_C
        else -> android.graphics.ColorSpace.Named.SRGB
    }
    return android.graphics.ColorSpace.get(frameworkNamedSpace)
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun android.graphics.ColorSpace.toComposeColorSpace(): ColorSpace {
    return when (this) {
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB)
            -> ColorSpaces.Srgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ACES)
            -> ColorSpaces.Aces
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ACESCG)
            -> ColorSpaces.Acescg
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ADOBE_RGB)
            -> ColorSpaces.AdobeRgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.BT2020)
            -> ColorSpaces.Bt2020
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.BT709)
            -> ColorSpaces.Bt709
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.CIE_LAB)
            -> ColorSpaces.CieLab
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.CIE_XYZ)
            -> ColorSpaces.CieXyz
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DCI_P3)
            -> ColorSpaces.DciP3
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DISPLAY_P3)
            -> ColorSpaces.DisplayP3
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.EXTENDED_SRGB)
            -> ColorSpaces.ExtendedSrgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_EXTENDED_SRGB)
            -> ColorSpaces.LinearExtendedSrgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_SRGB)
            -> ColorSpaces.LinearSrgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.NTSC_1953)
            -> ColorSpaces.Ntsc1953
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.PRO_PHOTO_RGB)
            -> ColorSpaces.ProPhotoRgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SMPTE_C)
            -> ColorSpaces.SmpteC
        else -> ColorSpaces.Srgb
    }
}