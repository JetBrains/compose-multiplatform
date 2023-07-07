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

package androidx.compose.animation.demos.layoutanimation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Preview
@Composable
fun AnimatedPlacementDemo() {
    var alignment by remember { mutableStateOf(Alignment.TopStart) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { alignment = Alignment.TopStart }) {
                Text("TopStart")
            }
            Button(onClick = { alignment = Alignment.TopCenter }) {
                Text("TopCenter")
            }
            Button(onClick = { alignment = Alignment.TopEnd }) {
                Text("TopEnd")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { alignment = Alignment.CenterStart }) {
                Text("CenterStart")
            }
            Button(onClick = { alignment = Alignment.Center }) {
                Text("Center")
            }
            Button(onClick = { alignment = Alignment.CenterEnd }) {
                Text("CenterEnd")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { alignment = Alignment.BottomStart }) {
                Text("BottomStart")
            }
            Button(onClick = { alignment = Alignment.BottomCenter }) {
                Text("BottomCenter")
            }
            Button(onClick = { alignment = Alignment.BottomEnd }) {
                Text("BottomEnd")
            }
        }
        AnimatedChildAlignment(alignment)
    }
}

fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    val modifier = remember { AnimatedPlacementModifier(scope) }
    this.then(modifier)
}

@OptIn(ExperimentalComposeUiApi::class)
class AnimatedPlacementModifier(val scope: CoroutineScope) : OnPlacedModifier, LayoutModifier {
    var targetOffset by mutableStateOf(Offset.Zero)
    var animatable by mutableStateOf<Animatable<Offset, AnimationVector2D>?>(null)
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(
                animatable?.let { (it.value - targetOffset).round() } ?: IntOffset.Zero
            )
        }
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        targetOffset = coordinates.positionInParent()
        // Animate to the new target offset when alignment changes.
        val anim = animatable ?: Animatable(targetOffset, Offset.VectorConverter)
            .also { animatable = it }
        if (anim.targetValue != targetOffset) {
            scope.launch {
                anim.animateTo(targetOffset, spring(stiffness = Spring.StiffnessMediumLow))
            }
        }
    }
}

@Composable
fun AnimatedChildAlignment(alignment: Alignment) {
    Box(Modifier.fillMaxSize().padding(4.dp).border(1.dp, Color.Red)) {
        Box(
            modifier = Modifier.animatePlacement().align(alignment).size(100.dp)
                .background(Color.Red)
        )
    }
}