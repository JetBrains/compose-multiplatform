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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.util.fastForEach

@OptIn(ExperimentalTextApi::class)
internal actual fun MultiParagraph.drawMultiParagraph(
    canvas: Canvas,
    brush: Brush,
    alpha: Float,
    shadow: Shadow?,
    decoration: TextDecoration?
) {
    canvas.save()
    paragraphInfoList.fastForEach {
        it.paragraph.paint(canvas, brush, alpha, shadow, decoration)
        canvas.translate(0f, it.paragraph.height)
    }
    canvas.restore()
}