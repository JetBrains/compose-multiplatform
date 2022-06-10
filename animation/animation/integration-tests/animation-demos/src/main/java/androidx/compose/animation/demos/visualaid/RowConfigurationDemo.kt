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

package androidx.compose.animation.demos.visualaid

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Preview
@Composable
fun RowConfigurationDemo() {
    val width by produceState(250.dp) {
        // Skip the animations in tests.
        while (coroutineContext[InfiniteAnimationPolicy] == null) {
            animate(
                Dp.VectorConverter, 250.dp, 520.dp,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
            ) { value, _ ->
                this.value = value
            }
            delay(1000)
            animate(
                Dp.VectorConverter, 520.dp, 250.dp,
                animationSpec = tween(520)
            ) { value, _ ->
                this.value = value
            }
            delay(1000)
        }
    }
    ResizableLayout(width)
}

@Composable
fun ResizableLayout(width: Dp) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.background(Color.White, RoundedCornerShape(5.dp))
        ) {
            Text(text = "Equal Weight", modifier = Modifier.padding(12.dp))
            Text(text = "Space Between", modifier = Modifier.padding(12.dp))
            Text(text = "Space Around", modifier = Modifier.padding(12.dp))
            Text(text = "Space Evenly", modifier = Modifier.padding(12.dp))
            Text(text = "End (LTR)", modifier = Modifier.padding(12.dp))
            Text(text = "Center", modifier = Modifier.padding(12.dp))
            Text(text = "Start (LTR)", modifier = Modifier.padding(12.dp))
        }
        Column(Modifier.weight(1f).requiredWidth(width)) {
            Row(Modifier.default(androidBlue)) {
                RowItem("A", fixedSize = false)
                RowItem("B", fixedSize = false)
                RowItem("C", fixedSize = false)
            }
            Row(Modifier.default(androidDark), horizontalArrangement = Arrangement.SpaceBetween) {
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
            }
            Row(Modifier.default(androidGreen), horizontalArrangement = Arrangement.SpaceAround) {
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
            }
            Row(Modifier.default(androidBlue), horizontalArrangement = Arrangement.SpaceEvenly) {
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
            }
            Row(Modifier.default(androidDark), horizontalArrangement = Arrangement.End) {
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
            }
            Row(Modifier.default(androidGreen), horizontalArrangement = Arrangement.Center) {
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
            }
            Row(Modifier.default(androidBlue), horizontalArrangement = Arrangement.Start) {
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
            }
        }
    }
}

private fun Modifier.default(background: Color) = this.padding(2.dp)
    .background(background, RoundedCornerShape(5.dp)).padding(start = 3.dp, end = 3.dp)
    .fillMaxWidth()

@Composable
fun RowScope.RowItem(text: String, fixedSize: Boolean = true) {
    val modifier = if (fixedSize) Modifier.width(80.dp) else Modifier.weight(1f)
    Box(
        modifier.padding(5.dp).shadow(10.dp)
            .background(Color.White, shape = RoundedCornerShape(5.dp))
            .padding(top = 5.dp, bottom = 5.dp)
    ) {
        Text(text, Modifier.align(Alignment.Center), color = androidDark)
    }
}

internal val androidGreen = Color(0xff3ddb85)
internal val androidDark = Color(0xff083042)
