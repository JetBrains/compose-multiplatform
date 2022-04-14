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

package androidx.compose.ui.text.font.testutils

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.AndroidFontLoader
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.TypefaceRequest
import androidx.compose.ui.text.font.TypefaceRequestCache
import androidx.compose.ui.text.font.TypefaceResult
import androidx.compose.ui.text.font.TypefaceResult.Immutable
import com.google.common.truth.Truth.assertThat

/**
 * Cheat to validate cache behavior in FontFamilyResolver
 *
 * Throws if cache contains [TypefaceResult.Async]
 *
 * @return null if cache miss, otherwise result if [TypefaceResult.Immutable]
 */
@OptIn(ExperimentalTextApi::class)
internal fun TypefaceRequestCache.getImmutableResultFor(
    fontFamily: FontFamily,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    fontSynthesis: FontSynthesis = FontSynthesis.All,
    fontLoader: AndroidFontLoader
): Any? {
    val result = get(
        TypefaceRequest(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            resourceLoaderCacheKey = fontLoader.cacheKey
        )
    )
    if (result == null) {
        return result
    }
    assertThat(result).isInstanceOf(Immutable::class.java)
    return result.value
}
