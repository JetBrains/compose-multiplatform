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

package androidx.compose.animation.demos.vectorgraphics

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.demos.R
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun AnimatedVectorGraphicsDemo() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val image = AnimatedImageVector.animatedVectorResource(R.drawable.ic_hourglass_animated)
        var atEnd by remember { mutableStateOf(false) }
        Image(
            painter = rememberAnimatedVectorPainter(image, atEnd),
            contentDescription = "AnimatedImageVector",
            modifier = Modifier.size(200.dp).clickable {
                atEnd = !atEnd
            },
            contentScale = ContentScale.Crop
        )

        var toggle by remember { mutableStateOf(false) }
        Image(
            painter = createSampleVectorPainter(toggle),
            contentDescription = "Transition with vector graphics",
            modifier = Modifier.size(200.dp).clickable {
                toggle = !toggle
            },
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun createSampleVectorPainter(toggle: Boolean): Painter {
    return rememberVectorPainter(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        name = "sample",
        autoMirror = true
    ) { _, _ ->
        val transition = updateTransition(targetState = toggle, label = "sample")
        val duration = 3000
        Path(
            pathData = PathData {
                horizontalLineTo(24f)
                verticalLineTo(24f)
                horizontalLineTo(0f)
                close()
            },
            fill = SolidColor(Color.Cyan)
        )
        val rotation by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    keyframes {
                        durationMillis = duration
                        0f at 0
                        360f at duration with LinearEasing
                    }
                } else {
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessVeryLow
                    )
                }
            },
            label = "rotation"
        ) { state ->
            if (state) 360f else 0f
        }

        @Suppress("UnusedTransitionTargetStateParameter")
        val translationX by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    keyframes {
                        durationMillis = duration
                        -6f at 500
                        6f at 1500
                        -6f at 2000
                        6f at 2500
                    }
                } else {
                    spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                }
            },
            label = "translationX"
        ) { 0f }

        @Suppress("UnusedTransitionTargetStateParameter")
        val translationY by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    keyframes {
                        durationMillis = duration
                        -6f at 1000
                        6f at 2000
                    }
                } else {
                    spring()
                }
            },
            label = "translationY"
        ) { 0f }
        Group(
            name = "rectangle",
            rotation = rotation,
            translationX = translationX,
            translationY = translationY,
            pivotX = 12f,
            pivotY = 12f
        ) {
            val fillColor by transition.animateColor(
                transitionSpec = {
                    if (targetState) {
                        keyframes {
                            durationMillis = duration
                            Color.Red at 0
                            Color.Blue at duration with LinearEasing
                        }
                    } else {
                        spring()
                    }
                },
                label = "fillColor"
            ) { state ->
                if (state) Color.Blue else Color.Red
            }
            Path(
                pathData = PathData {
                    moveTo(8f, 8f)
                    lineTo(16f, 8f)
                    lineTo(16f, 16f)
                    lineTo(8f, 16f)
                    close()
                },
                fill = SolidColor(fillColor)
            )
        }
    }
}
