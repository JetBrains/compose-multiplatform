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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun CanvasSample() {
    Canvas(modifier = Modifier.size(100.dp)) {
        drawRect(Color.Magenta)
        inset(10.0f) {
            drawLine(
                color = Color.Red,
                start = Offset.Zero,
                end = Offset(size.width, size.height),
                strokeWidth = 5.0f
            )
        }
    }
}

@Sampled
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CanvasPieChartSample() {
    Canvas(
        contentDescription = "Pie chart: 80% apples, 20% bananas (localized string)",
        modifier = Modifier.size(300.dp)
    ) {
        // Apples (80%)
        drawCircle(
            color = Color.Red,
            radius = size.width / 2
        )

        // Bananas (20%)
        drawArc(
            color = Color.Yellow,
            startAngle = 0f,
            sweepAngle = 360f * 0.20f,
            useCenter = true,
            topLeft = Offset(0f, (size.height - size.width) / 2f),
            size = Size(size.width, size.width)
        )
    }
}
