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
package androidx.compose.ui.unit.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun DpSample() {
    Box(
        Modifier.padding(
            10.dp, // Int
            10f.dp, // Float
            20.0.dp, // Double
            10.dp
        )
    )
}

@Sampled
@Composable
fun ToPxSample() {
    val lineThickness = 6.dp
    Canvas(Modifier.fillMaxSize()) {
        val lineThicknessPx = lineThickness.toPx()
        inset(lineThicknessPx / 2) {
            drawRect(Color.Red, style = Stroke(lineThicknessPx))
        }
    }
}