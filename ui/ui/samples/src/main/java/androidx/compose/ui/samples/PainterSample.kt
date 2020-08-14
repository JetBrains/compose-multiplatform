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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun PainterModifierSample() {
    class CustomPainter : Painter() {

        override val intrinsicSize: Size
            get() = Size(300.0f, 300.0f)

        override fun DrawScope.onDraw() {
            drawCircle(
                center = center,
                radius = size.minDimension / 2.0f,
                color = Color.Red
            )
        }
    }

    Box(
        modifier =
            Modifier.background(color = Color.Gray)
                .padding(30.dp)
                .background(color = Color.Yellow)
                .paint(CustomPainter())
    ) { /** intentionally empty **/ }
}