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

package androidx.compose.ui.text.platform

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.style.TextDecoration

/**
 * Brush is applied to the whole text whether it's MultiParagraph or just Paragraph. However, Brush
 * needs to be realized as a shader before being applied to the text. Applying the same brush
 * separately to each Paragraph in a MultiParagraph violates the unity of the text.
 *
 * This function draws a MultiParagraph with the given brush without repeating the same effect in
 * each Paragraph. Drawing MultiParagraph is expected to be implemented differently in each platform
 * since Shader implementation is also heavily coupled to the underlying platform.
 */
internal expect fun MultiParagraph.drawMultiParagraph(
    canvas: Canvas,
    brush: Brush,
    alpha: Float = Float.NaN,
    shadow: Shadow? = null,
    decoration: TextDecoration? = null
)