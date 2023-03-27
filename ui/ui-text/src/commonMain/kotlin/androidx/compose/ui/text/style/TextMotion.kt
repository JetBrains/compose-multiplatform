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

package androidx.compose.ui.text.style

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi

/**
 * Defines ways to render and place glyphs to provide readability and smooth animations for text.
 */
@ExperimentalTextApi
@Immutable
expect class TextMotion {
    companion object {
        /**
         * Optimizes glyph shaping, placement, and overall rendering for maximum readability.
         * Intended for text that is not animated. This is the default [TextMotion].
         */
        val Static: TextMotion

        /**
         * Text is rendered for maximum linearity which provides smooth animations for text.
         * Trade-off is the readability of the text on some low DPI devices, which still should not
         * be a major concern. Use this [TextMotion] if you are planning to scale, translate, or
         * rotate text.
         */
        val Animated: TextMotion
    }
}