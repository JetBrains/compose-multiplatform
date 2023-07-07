/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.font

import android.content.Context
import android.content.res.Configuration
import android.graphics.fonts.FontStyle
import android.os.Build
import androidx.compose.ui.unit.Density

/**
 * Android implementation for [PlatformResolveInterceptor].
 *
 * Android system accessibility should be able to override any font metric that
 * affects how FontFamily is resolved. Font Size preference is handled by [Density.fontScale].
 * This interceptor currently adjusts the Font Weight for Bold Text feature.
 */
internal data class AndroidFontResolveInterceptor(
    private val fontWeightAdjustment: Int
) : PlatformResolveInterceptor {

    override fun interceptFontWeight(fontWeight: FontWeight): FontWeight {
        // do not intercept if fontWeightAdjustment is not set or undefined
        if (fontWeightAdjustment == 0 ||
            fontWeightAdjustment == Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED) {
            return fontWeight
        }

        val finalFontWeight = (fontWeight.weight + fontWeightAdjustment).coerceIn(
            FontStyle.FONT_WEIGHT_MIN,
            FontStyle.FONT_WEIGHT_MAX
        )
        return FontWeight(finalFontWeight)
    }
}

/**
 * A helper function to create an interceptor using a context.
 */
internal fun AndroidFontResolveInterceptor(context: Context): AndroidFontResolveInterceptor {
    val fontWeightAdjustment = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.resources.configuration.fontWeightAdjustment
    } else {
        0
    }
    return AndroidFontResolveInterceptor(fontWeightAdjustment)
}