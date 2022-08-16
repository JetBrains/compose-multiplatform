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

package androidx.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * This value should reflect the default cache size for TextMeasurer.
 */
private val DefaultCacheSize: Int = 8

/**
 * Creates and remembers a [TextMeasurer]. All parameters that are required for TextMeasurer except
 * [cacheSize] are read from CompositionLocals. Created TextMeasurer carries an internal
 * [TextLayoutCache] with [cacheSize] capacity. Provide 0 for [cacheSize] to opt-out from internal
 * caching behavior. Moreover, the cache can be disabled at will during measure by passing in
 * skipCache as true.
 *
 * @param cacheSize Capacity of internal cache inside TextMeasurer. Size unit is the number of
 * unique text layout inputs that are measured.
 */
@ExperimentalTextApi
@Composable
fun rememberTextMeasurer(
    cacheSize: Int = DefaultCacheSize
): TextMeasurer {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    return remember(fontFamilyResolver, density, layoutDirection, cacheSize) {
        TextMeasurer(fontFamilyResolver, density, layoutDirection, cacheSize)
    }
}