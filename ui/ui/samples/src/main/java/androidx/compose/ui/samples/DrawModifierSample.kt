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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.dp

/**
 * Sample showing how to leverage [Modifier.drawWithCache] in order
 * to cache contents in between draw calls that depend on sizing information.
 * In the example below, the LinearGradient is created once and re-used across
 * calls to onDraw. If the size of the drawing area changes, then the
 * LinearGradient is re-created with the updated width and height.
 */
@Sampled
@Composable
fun DrawWithCacheModifierSample() {
    Box(
        Modifier.drawWithCache {
            val gradient = Brush.linearGradient(
                colors = listOf(Color.Red, Color.Blue),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            onDrawBehind {
                drawRect(gradient)
            }
        }
    )
}

/**
 * Sample showing how to leverage [Modifier.drawWithCache] to persist data across
 * draw calls. In the example below, the linear gradient will be re-created if either
 * the size of the drawing area changes, or the toggle flag represented by a mutable state
 * object changes. Otherwise the same linear gradient instance is re-used for each call
 * to drawRect.
 */
@Sampled
@Composable
fun DrawWithCacheModifierStateParameterSample() {
    val colors1 = listOf(Color.Red, Color.Blue)
    val colors2 = listOf(Color.Yellow, Color.Green)
    var toggle by remember { mutableStateOf(true) }
    Box(
        Modifier.clickable { toggle = !toggle }.drawWithCache {
            val gradient = Brush.linearGradient(
                colors = if (toggle) colors1 else colors2,
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            onDrawBehind {
                drawRect(gradient)
            }
        }
    )
}

/**
 * Sample showing how to leverage [Modifier.drawWithCache] to cache a LinearGradient
 * if the size is unchanged. Additionally this sample illustrates how to re-arrange
 * drawing order using [ContentDrawScope.drawContent] in order to draw the desired
 * content first to support blending against the sample vector graphic of a triangle
 */
@Sampled
@Composable
fun DrawWithCacheContentSample() {
    val vectorPainter = rememberVectorPainter(24.dp, 24.dp, autoMirror = true) {
            viewportWidth, viewportHeight ->
        Path(
            pathData = PathData {
                lineTo(viewportWidth, 0f)
                lineTo(0f, viewportHeight)
                close()
            },
            fill = SolidColor(Color.Black)
        )
    }
    Image(
        painter = vectorPainter,
        contentDescription = null,
        modifier = Modifier.requiredSize(120.dp).drawWithCache {
            val gradient = Brush.linearGradient(
                colors = listOf(Color.Red, Color.Blue),
                start = Offset.Zero,
                end = Offset(0f, size.height)
            )
            onDrawWithContent {
                drawContent()
                drawRect(gradient, blendMode = BlendMode.Plus)
            }
        }
    )
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun DrawModifierNodeSample() {
    class CircleNode(var color: Color) : DrawModifierNode, Modifier.Node() {
        override fun ContentDrawScope.draw() {
            drawCircle(color)
        }
    }
    data class CircleElement(val color: Color) : ModifierNodeElement<CircleNode>() {
        override fun create() = CircleNode(color)
        override fun update(node: CircleNode): CircleNode {
            node.color = color
            return node
        }
        override fun InspectorInfo.inspectableProperties() {
            name = "color"
            properties["color"] = color
        }
    }
    fun Modifier.circle(color: Color) = this then CircleElement(color)
    Box(Modifier.fillMaxSize().circle(Color.Blue))
}