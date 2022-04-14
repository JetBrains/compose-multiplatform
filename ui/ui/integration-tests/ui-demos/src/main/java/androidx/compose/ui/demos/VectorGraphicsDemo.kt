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

package androidx.compose.ui.demos

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VectorGraphicsDemo() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageVector = painterResource(R.drawable.ic_crane)
        Image(
            painter = imageVector,
            contentDescription = "Crane",
            modifier = Modifier.size(200.dp, 200.dp),
            contentScale = ContentScale.Inside
        )

        val complexImageVector = painterResource(R.drawable.ic_hourglass)
        Image(
            painter = complexImageVector,
            contentDescription = "Hourglass",
            modifier = Modifier.size(64.dp, 64.dp),
            contentScale = ContentScale.Fit
        )

        Image(
            painter = vectorShape(120.dp, 120.dp),
            contentDescription = null,
            modifier = Modifier.size(200.dp, 150.dp)
        )
    }
}

@Composable
private fun vectorShape(width: Dp, height: Dp): Painter =
    rememberVectorPainter(
        name = "vectorShape",
        defaultWidth = width,
        defaultHeight = height,
        autoMirror = false
    ) { viewportWidth, viewportHeight ->
        Group(
            scaleX = 0.75f,
            scaleY = 0.75f,
            rotation = 45.0f,
            pivotX = (viewportWidth / 2),
            pivotY = (viewportHeight / 2)
        ) {
            BackgroundPath(viewportWidth, viewportHeight)
            StripePath(viewportWidth, viewportHeight)
            Group(
                translationX = 50.0f,
                translationY = 50.0f,
                pivotX = (viewportWidth / 2),
                pivotY = (viewportHeight / 2),
                rotation = 25.0f
            ) {
                val pathData = PathData {
                    moveTo(viewportWidth / 2 - 100, viewportHeight / 2 - 100)
                    horizontalLineToRelative(200.0f)
                    verticalLineToRelative(200.0f)
                    horizontalLineToRelative(-200.0f)
                    close()
                }
                Path(
                    fill = Brush.horizontalGradient(
                        listOf(
                            Color.Red,
                            Color.Blue
                        ),
                        startX = 0.0f,
                        endX = viewportWidth / 2 + 100.0f
                    ),
                    pathData = pathData
                )
            }
            Triangle()
            TriangleWithOffsets()
        }
    }

@Composable
private fun BackgroundPath(vectorWidth: Float, vectorHeight: Float) {
    val background = PathData {
        horizontalLineTo(vectorWidth)
        verticalLineTo(vectorHeight)
        horizontalLineTo(0.0f)
        close()
    }

    Path(
        fill = Brush.verticalGradient(
            0.0f to Color.Cyan,
            0.3f to Color.Green,
            1.0f to Color.Magenta,
            startY = 0.0f,
            endY = vectorHeight,
            tileMode = TileMode.Clamp
        ),
        pathData = background
    )
}

@Composable
private fun Triangle() {
    val length = 150.0f
    Path(
        fill = Brush.radialGradient(
            listOf(
                Color(0xFF000080),
                Color(0xFF808000),
                Color(0xFF008080)
            ),
            Offset(length / 2.0f, length / 2.0f),
            radius = length / 2.0f,
            tileMode = TileMode.Repeated
        ),
        pathData = PathData {
            verticalLineTo(length)
            horizontalLineTo(length)
            close()
        }
    )
}

@Composable
private fun TriangleWithOffsets() {

    val side1 = 150.0f
    val side2 = 150.0f
    Path(
        fill = Brush.radialGradient(
            0.0f to Color(0xFF800000),
            0.3f to Color.Cyan,
            0.8f to Color.Yellow,
            center = Offset(side1 / 2.0f, side2 / 2.0f),
            radius = side1 / 2.0f,
            tileMode = TileMode.Clamp
        ),
        pathData = PathData {
            horizontalLineToRelative(side1)
            verticalLineToRelative(side2)
            close()
        }
    )
}

@Composable
private fun StripePath(vectorWidth: Float, vectorHeight: Float) {
    val stripeDelegate = PathData {
        stripe(vectorWidth, vectorHeight, 10)
    }

    Path(stroke = SolidColor(Color.Blue), pathData = stripeDelegate)
}

private fun PathBuilder.stripe(vectorWidth: Float, vectorHeight: Float, numLines: Int) {
    val stepSize = vectorWidth / numLines
    var currentStep = stepSize
    for (i in 1..numLines) {
        moveTo(currentStep, 0.0f)
        verticalLineTo(vectorHeight)
        currentStep += stepSize
    }
}
