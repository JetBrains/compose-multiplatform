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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.fontResource
import androidx.compose.ui.res.loadFontResource
import androidx.compose.ui.text.font.FontFamily

@Sampled
@Composable
fun FontResourcesFontFamily() {
    loadFontResource(
        FontFamily.Default,
        pendingFontFamily = FontFamily.Cursive,
        failedFontFamily = FontFamily.SansSerif
    ).resource.resource?.let {
        Text(
            text = "Hello, World",
            fontFamily = FontFamily(it)
        )
    }
}

@Sampled
@Composable
fun FontResourcesTypeface() {
    loadFontResource(
        FontFamily.Default,
        pendingTypeface = fontResource(FontFamily.Cursive),
        failedTypeface = fontResource(FontFamily.SansSerif)
    ).resource.resource?.let {
        Text(
            text = "Hello, World",
            fontFamily = FontFamily(it)
        )
    }
}
