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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val minWidth = 240.dp

@Composable
fun RowConfigurationDemo() {
    val width by produceState(minWidth) {
        // Skip the animations in tests.
        while (coroutineContext[InfiniteAnimationPolicy] == null) {
            animate(
                Dp.VectorConverter, minWidth, 500.dp,
                animationSpec = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessLow)
            ) { value, _ ->
                this.value = value
            }
            animate(
                Dp.VectorConverter, 500.dp, minWidth,
                animationSpec = tween(500)
            ) { value, _ ->
                this.value = value
            }
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
            Text(text = "Align to End", modifier = Modifier.padding(12.dp))
            Text(text = "Center", modifier = Modifier.padding(12.dp))
            Text(text = "Align to Start", modifier = Modifier.padding(12.dp))
        }
        Column(Modifier.weight(1f).requiredWidth(width)) {
            val rowModifier =
                Modifier.padding(2.dp).background(Color.Blue, RoundedCornerShape(5.dp))
            Row(rowModifier.fillMaxWidth()) {
                RowItem(Modifier.weight(1f), "A")
                RowItem(Modifier.weight(1f), "B")
                RowItem(Modifier.weight(1f), "C")
            }
            Row(rowModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
            }
            Row(rowModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
            }
            Row(rowModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
            }
            Row(rowModifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
            }
            Row(rowModifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
                Spacer(Modifier.weight(1f))
            }
            Row(rowModifier.fillMaxWidth()) {
                RowItem(text = "A")
                RowItem(text = "B")
                RowItem(text = "C")
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RowItem(modifier: Modifier = Modifier, text: String, background: Color = Color.Yellow) {
    Box(
        modifier.padding(5.dp).shadow(10.dp)
            .background(background, shape = RoundedCornerShape(5.dp))
            .padding(start = 30.dp, end = 30.dp, top = 5.dp, bottom = 5.dp)
    ) {
        Text(text, Modifier.align(Alignment.Center))
    }
}
