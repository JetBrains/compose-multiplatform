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

package androidx.compose.animation.demos

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Preview
@Composable
fun ColumnConfigurationDemo() {
    val height by produceState(250.dp) {
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
    ResizableColumn(height)
}

@Composable
fun ResizableColumn(height: Dp) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.background(Color.White, RoundedCornerShape(5.dp)).padding(10.dp)
        ) {
            Text(
                text = "Equal\nWeight",
                modifier = Modifier.width(50.dp).padding(2.dp),
                fontSize = 12.sp, textAlign = TextAlign.Center
            )
            Text(
                text = "Space\nBetween",
                modifier = Modifier.width(50.dp).padding(2.dp),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Space\nAround",
                modifier = Modifier.width(50.dp).padding(2.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Space\nEvenly",
                modifier = Modifier.width(50.dp).padding(2.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Top",
                modifier = Modifier.width(50.dp).padding(2.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Center",
                modifier = Modifier.width(50.dp).padding(2.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Bottom",
                modifier = Modifier.width(50.dp).padding(2.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        Row(Modifier.height(520.dp).requiredHeight(height)) {
            Column(Modifier.default(androidBlue)) {
                ColumnItem("A", fixedSize = false)
                ColumnItem("B", fixedSize = false)
                ColumnItem("C", fixedSize = false)
            }
            Column(Modifier.default(androidDark), verticalArrangement = Arrangement.SpaceBetween) {
                ColumnItem(text = "A")
                ColumnItem(text = "B")
                ColumnItem(text = "C")
            }
            Column(Modifier.default(androidGreen), verticalArrangement = Arrangement.SpaceAround) {
                ColumnItem(text = "A")
                ColumnItem(text = "B")
                ColumnItem(text = "C")
            }
            Column(Modifier.default(androidBlue), verticalArrangement = Arrangement.SpaceEvenly) {
                ColumnItem(text = "A")
                ColumnItem(text = "B")
                ColumnItem(text = "C")
            }
            Column(Modifier.default(androidDark), verticalArrangement = Arrangement.Top) {
                ColumnItem(text = "A")
                ColumnItem(text = "B")
                ColumnItem(text = "C")
            }
            Column(Modifier.default(androidGreen), verticalArrangement = Arrangement.Center) {
                ColumnItem(text = "A")
                ColumnItem(text = "B")
                ColumnItem(text = "C")
            }
            Column(Modifier.default(androidBlue), verticalArrangement = Arrangement.Bottom) {
                ColumnItem(text = "A")
                ColumnItem(text = "B")
                ColumnItem(text = "C")
            }
        }
    }
}

private fun Modifier.default(backgroundColor: Color) =
    this.width(50.dp).padding(2.dp)
        .background(backgroundColor, RoundedCornerShape(5.dp)).padding(top = 3.dp, bottom = 3.dp)
        .fillMaxHeight()

@Composable
fun ColumnScope.ColumnItem(text: String, fixedSize: Boolean = true) {
    val modifier = if (fixedSize) Modifier.height(80.dp) else Modifier.weight(1f)
    Box(
        modifier.width(50.dp).padding(5.dp).shadow(10.dp)
            .background(Color.White, shape = RoundedCornerShape(5.dp))
            .padding(top = 5.dp, bottom = 5.dp)
    ) {
        Text(text, Modifier.align(Alignment.Center), color = androidDark)
    }
}

val androidBlue = Color(0xff4282f2)