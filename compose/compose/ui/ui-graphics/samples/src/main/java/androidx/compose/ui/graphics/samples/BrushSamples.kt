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

package androidx.compose.ui.graphics.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun GradientBrushSample() {
    Column(modifier = Modifier.fillMaxSize().wrapContentSize()) {

        // Create a linear gradient that shows red in the top left corner
        // and blue in the bottom right corner
        val linear = Brush.linearGradient(listOf(Color.Red, Color.Blue))

        Box(modifier = Modifier.size(120.dp).background(linear))

        Spacer(modifier = Modifier.size(20.dp))

        // Create a radial gradient centered about the drawing area that is green on
        // the outer
        // edge of the circle and magenta towards the center of the circle
        val radial = Brush.radialGradient(listOf(Color.Green, Color.Magenta))
        Box(modifier = Modifier.size(120.dp).background(radial))

        Spacer(modifier = Modifier.size(20.dp))

        // Create a radial gradient centered about the drawing area that is green on
        // the outer
        // edge of the circle and magenta towards the center of the circle
        val sweep = Brush.sweepGradient(listOf(Color.Cyan, Color.Magenta))
        Box(modifier = Modifier.size(120.dp).background(sweep))
    }
}