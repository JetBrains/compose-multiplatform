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
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun ChangeOpacity() {
    Text("Hello World", Modifier.graphicsLayer(alpha = 0.5f, clip = true))
}

@Sampled
@Composable
fun AnimateFadeIn() {
    val animatedAlpha = remember { Animatable(0f) }
    Text(
        "Hello World",
        Modifier.graphicsLayer {
            alpha = animatedAlpha.value
            clip = true
        }
    )
    LaunchedEffect(animatedAlpha) {
        animatedAlpha.animateTo(1f)
    }
}

@Sampled
@Composable
fun CompositingStrategyModulateAlpha() {
    Canvas(
        modifier =
            Modifier.size(100.dp)
            .background(Color.Black)
            .graphicsLayer(
                alpha = 0.5f,
                compositingStrategy = CompositingStrategy.ModulateAlpha
            )
    ) {
        // Configuring an alpha less than 1.0 and specifying
        // CompositingStrategy.ModulateAlpha ends up with the overlapping region
        // of the 2 draw rect calls to blend transparent blue and transparent red
        // against the black background instead of just transparent blue which is what would
        // occur with CompositingStrategy.Auto or CompositingStrategy.Offscreen
        inset(0f, 0f, size.width / 3, size.height / 3) {
            drawRect(color = Color.Red)
        }
        inset(size.width / 3, size.height / 3, 0f, 0f) {
            drawRect(color = Color.Blue)
        }
    }
}
