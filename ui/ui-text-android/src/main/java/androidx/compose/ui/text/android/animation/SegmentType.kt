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

package androidx.compose.ui.text.android.animation

import androidx.compose.ui.text.android.InternalPlatformTextApi

/**
 * Defines a segmentation rule for text animation
 *
 * @suppress
 */
@InternalPlatformTextApi
enum class SegmentType {
    /**
     * Don't break text and treat whole text as the segment.
     */
    Document,

    /**
     * Break text with paragraph breaker.
     */
    Paragraph,

    /**
     * Break text with automated line break position.
     */
    Line,

    /**
     * Break text with word boundary.
     *
     * Note that this uses line breaking instance of the break iterator.
     * Also this includes Bidi transition offset.
     */
    Word,

    /**
     * Break text with character (grapheme) boundary.
     */
    Character
}