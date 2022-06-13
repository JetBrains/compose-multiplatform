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

package androidx.compose.ui.text.platform.style

import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.platform.setAlpha

/**
 * A span that applies [ShaderBrush] to TextPaint after receiving a specified size
 */
internal class ShaderBrushSpan(
    val shaderBrush: ShaderBrush,
    val alpha: Float
) : CharacterStyle(), UpdateAppearance {
    var size: Size? = null

    override fun updateDrawState(textPaint: TextPaint?) {
        if (textPaint != null) {
            size?.let {
                textPaint.shader = shaderBrush.createShader(it)
            }
            textPaint.setAlpha(alpha)
        }
    }
}
