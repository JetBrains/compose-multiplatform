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

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalTextApi::class)

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Sampled
@Composable
fun BasicMarqueeSample() {
    // Marquee only animates when the content doesn't fit in the max width.
    Column(Modifier.width(30.dp)) {
        Text("hello world", Modifier.basicMarquee())
    }
}

@Preview(showBackground = true)
@Sampled
@Composable
fun BasicFocusableMarqueeSample() {
    val focusRequester = remember { FocusRequester() }

    // Marquee only animates when the content doesn't fit in the max width.
    Column(Modifier.width(30.dp)) {
        Text("hello world", Modifier
            .clickable { focusRequester.requestFocus() }
            .basicMarquee(animationMode = MarqueeAnimationMode.WhileFocused)
            .focusRequester(focusRequester)
            .focusable()
        )
    }
}

@Preview(showBackground = true)
@Sampled
@Composable
fun BasicMarqueeWithFadedEdgesSample() {
    val edgeWidth = 32.dp
    fun ContentDrawScope.drawFadedEdge(leftEdge: Boolean) {
        val edgeWidthPx = edgeWidth.toPx()
        drawRect(
            topLeft = Offset(if (leftEdge) 0f else size.width - edgeWidthPx, 0f),
            size = Size(edgeWidthPx, size.height),
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = if (leftEdge) 0f else size.width,
                endX = if (leftEdge) edgeWidthPx else size.width - edgeWidthPx
            ),
            blendMode = BlendMode.DstIn
        )
    }

    Text(
        "the quick brown fox jumped over the lazy dogs",
        Modifier
            .widthIn(max = edgeWidth * 4)
            // Rendering to an offscreen buffer is required to get the faded edges' alpha to be
            // applied only to the text, and not whatever is drawn below this composable (e.g. the
            // window).
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawFadedEdge(leftEdge = true)
                drawFadedEdge(leftEdge = false)
            }
            .basicMarquee(
                // Animate forever.
                iterations = Int.MAX_VALUE,
                spacing = MarqueeSpacing(0.dp)
            )
            .padding(start = edgeWidth)
    )
}