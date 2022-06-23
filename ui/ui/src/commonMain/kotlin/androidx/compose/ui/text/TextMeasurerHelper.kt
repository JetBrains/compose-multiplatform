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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * This value should reflect the default cache size for TextMeasurer.
 */
private val DefaultCacheSize: Int = 8

/**
 * Creates and remembers a [TextMeasurer] that reads default values for optional parameters from
 * CompositionLocals. Returned TextMeasurer also carries an internal [TextLayoutCache] at a given
 * capacity. Provide 0 as capacity to opt-out from internal caching behavior.
 *
 * All given parameters can be overridden during a [TextMeasurer.measure] call except the maximum
 * size of the TextLayoutCache. Instead the cache can be disabled at will during measure by passing
 * in skipCache as true.
 *
 * @param fontFamilyResolver default [FontFamily.Resolver] to be used to load the font given
 * in [SpanStyle]s.
 * @param density default density.
 * @param layoutDirection default layout direction.
 * @param cacheSize Capacity of internal cache inside TextMeasurer.
 */
@ExperimentalTextApi
@Composable
fun rememberTextMeasurer(
    fontFamilyResolver: FontFamily.Resolver = LocalFontFamilyResolver.current,
    density: Density = LocalDensity.current,
    layoutDirection: LayoutDirection = LocalLayoutDirection.current,
    cacheSize: Int = DefaultCacheSize
): TextMeasurer {
    return remember(fontFamilyResolver, density, layoutDirection, cacheSize) {
        TextMeasurer(fontFamilyResolver, density, layoutDirection, cacheSize)
    }
}