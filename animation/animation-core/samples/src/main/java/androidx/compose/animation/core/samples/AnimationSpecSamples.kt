/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required = applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.animation.core.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun InfiniteProgressIndicator() {
    // This is an infinite progress indicator with 3 pulsing dots that grow and shrink.
    @Composable
    fun Dot(scale: State<Float>) {
        Box(
            Modifier.padding(5.dp).size(20.dp).graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }.background(Color.Gray, shape = CircleShape)
        )
    }

    val infiniteTransition = rememberInfiniteTransition()
    val scale1 = infiniteTransition.animateFloat(
        0.2f,
        1f,
        // No offset for the 1st animation
        infiniteRepeatable(tween(600), RepeatMode.Reverse)
    )
    val scale2 = infiniteTransition.animateFloat(
        0.2f,
        1f,
        infiniteRepeatable(
            tween(600), RepeatMode.Reverse,
            // Offsets the 2nd animation by starting from 150ms of the animation
            // This offset will not be repeated.
            initialStartOffset = StartOffset(offsetMillis = 150, StartOffsetType.FastForward)
        )
    )
    val scale3 = infiniteTransition.animateFloat(
        0.2f,
        1f,
        infiniteRepeatable(
            tween(600), RepeatMode.Reverse,
            // Offsets the 3rd animation by starting from 300ms of the animation. This
            // offset will be not repeated.
            initialStartOffset = StartOffset(offsetMillis = 300, StartOffsetType.FastForward)
        )
    )
    Row {
        Dot(scale1)
        Dot(scale2)
        Dot(scale3)
    }
}