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
package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun PlacementScopeCoordinatesSample() {
    // Layout so that the first item consumes to half of the width of the screen, if possible.
    // The remainder of the layouts are positioned horizontally in the remaining space.
    @Composable
    fun FirstItemHalf(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        val view = LocalView.current

        Layout(content = content, modifier = modifier, measurePolicy = { measurables, constraints ->
            var width = constraints.minWidth
            var height = constraints.minHeight
            // If this doesn't have a fixed size, just layout horizontally
            var placeables: List<Placeable>? = null
            if (measurables.isNotEmpty()) {
                if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
                    width = constraints.maxWidth
                    height = constraints.maxHeight
                } else {
                    placeables = measurables.map { it.measure(constraints) }
                    width = placeables.sumOf { it.width }
                    height = placeables.maxOf { it.height }
                }
            }
            layout(width, height) {
                if (placeables != null) {
                    var x = 0
                    placeables.forEach {
                        it.placeRelative(x, 0)
                        x += it.width
                    }
                } else if (measurables.isNotEmpty() && coordinates != null) {
                    val coordinates = coordinates!!
                    val positionInWindow = IntArray(2)
                    view.getLocationOnScreen(positionInWindow)
                    val topLeft = coordinates.localToRoot(Offset.Zero).round() +
                        IntOffset(positionInWindow[0], positionInWindow[1])
                    val displayWidth = view.resources.displayMetrics.widthPixels
                    val halfWay = displayWidth / 2

                    val c0 = if (topLeft.x < halfWay) {
                        // The first measurable should fit to half way across
                        Constraints.fixed(
                            halfWay - topLeft.x,
                            height
                        )
                    } else {
                        // The first is already past the half way, so just divide it evenly
                        val measureWidth = width / measurables.size
                        Constraints.fixed(measureWidth, height)
                    }
                    val p0 = measurables[0].measure(c0)
                    p0.place(0, 0)

                    // The rest just fit in the remainder of the space
                    var x = p0.width
                    for (i in 1..measurables.lastIndex) {
                        val measureWidth = (width - x) / (measurables.size - i)
                        val p = measurables[i].measure(Constraints.fixed(measureWidth, height))
                        p.place(x, 0)
                        x += p.width
                    }
                }
            }
        })
    }
}