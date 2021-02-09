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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

@Composable
fun DeclarativeGraphicsDemo() {
    /**
     * Demo that shows how to leverage DrawScope to draw 4 rectangular quadrants
     * inset by a given dimension with a diamond drawn within each of the quadrants
     */
    Canvas(
        modifier =
            Modifier.fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .size(120.dp, 120.dp)
    ) {
        drawRect(color = Color.Gray)
        // Inset content by 10 pixels on the left/right sides and 12 by the
        // top/bottom
        inset(10.0f, 12.0f) {
            val quadrantSize = size / 2.0f
            drawRect(
                size = quadrantSize,
                color = Color.Red
            )
            // Scale the drawing environment down by 50% about the center of the square drawn
            // in the top left quadrant
            scale(0.5f, Offset(size.width / 4, size.height / 4)) {
                // Rotate the drawing environment 45 degrees about the center of the square
                // drawn in the top left
                rotate(45.0f, Offset(size.width / 4, size.height / 4)) {
                    drawRect(
                        size = quadrantSize,
                        color = Color.Yellow,
                        alpha = 0.75f
                    )
                }
            }
            // Translate the drawing environment to the right by half the size of the current
            // width
            translate(size.width / 2, 0.0f) {
                drawRect(
                    size = quadrantSize,
                    color = Color.Yellow
                )
                // Scale the drawing environment down by 50% about the center of the square drawn
                // in the top right quadrant
                scale(0.5f, Offset(size.width / 4, size.height / 4)) {
                    // rotate the drawing environment 45 degrees about the center of the drawn
                    // square in the top right
                    rotate(45.0f, Offset(size.width / 4, size.height / 4)) {
                        drawRect(
                            size = quadrantSize,
                            color = Color.Red,
                            alpha = 0.75f
                        )
                    }
                }
            }
            // Translate the drawing environment down by half the size of the current height
            translate(0.0f, size.height / 2) {
                drawRect(
                    size = quadrantSize,
                    color = Color.Green
                )
                // Scale the drawing environment down by 50% about the center of the square drawn
                // in the bottom left quadrant
                scale(0.5f, Offset(size.width / 4, size.height / 4)) {
                    // Rotate the drawing environment by 45 degrees about the center of the
                    // square drawn in the bottom left quadrant
                    rotate(45.0f, Offset(size.width / 4, size.height / 4)) {
                        drawRect(
                            size = quadrantSize,
                            color = Color.Blue,
                            alpha = 0.75f
                        )
                    }
                }
            }
            // Translate the drawing environment to the bottom right quadrant of the inset bounds
            translate(size.width / 2, size.height / 2) {
                drawRect(
                    size = quadrantSize,
                    color = Color.Blue
                )
                // Scale the drawing environment down by 50% about the center of the square drawn
                // in the bottom right quadrant
                scale(0.5f, Offset(size.width / 4, size.height / 4)) {
                    // Rotate the drawing environment 45 degrees about the center of the drawn
                    // square in the bottom right
                    rotate(45.0f, Offset(size.width / 4, size.height / 4)) {
                        drawRect(
                            size = quadrantSize,
                            color = Color.Green,
                            alpha = 0.75f
                        )
                    }
                }
            }
        }
    }
}