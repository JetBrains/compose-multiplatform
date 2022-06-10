/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.animation.demos.statetransition

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun InfiniteTransitionDemo() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InfinitePulsingHeart()
        Spacer(Modifier.size(200.dp))
        InfiniteProgress()
    }
}

@Composable
fun InfinitePulsingHeart() {
    val infiniteTransition = rememberInfiniteTransition()

    val scale by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        )
    )

    val color by infiniteTransition.animateColor(
        initialValue = Color.Red,
        targetValue = Color(0xff800000), // Dark Red
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Icon(
        Icons.Filled.Favorite,
        null,
        Modifier.graphicsLayer(
            scaleX = scale,
            scaleY = scale
        ),
        tint = color
    )
}

@Composable
fun InfiniteProgress() {
    val infiniteTransition = rememberInfiniteTransition()
    Row {
        infiniteTransition.PulsingDot(StartOffset(0))
        infiniteTransition.PulsingDot(StartOffset(150, StartOffsetType.FastForward))
        infiniteTransition.PulsingDot(StartOffset(300, StartOffsetType.FastForward))
    }
}

@Composable
fun InfiniteTransition.PulsingDot(startOffset: StartOffset) {
    val scale by animateFloat(
        0.2f,
        1f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse, initialStartOffset = startOffset)
    )
    Box(
        Modifier.padding(5.dp).size(20.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.background(Color.Gray, shape = CircleShape)
    )
}
