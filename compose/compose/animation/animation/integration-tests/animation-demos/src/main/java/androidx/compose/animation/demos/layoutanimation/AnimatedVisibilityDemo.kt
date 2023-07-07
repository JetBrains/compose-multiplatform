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

package androidx.compose.animation.demos.layoutanimation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.demos.gesture.pastelColors
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.samples.AnimatedFloatingActionButton
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Preview
@Composable
fun AnimatedVisibilityDemo() {
    val animateContentSize = remember { mutableStateOf(false) }
    Column {
        AnimatedItems(animateContentSize.value)
        AnimateContentSizeOption(animateContentSize)
        AnimatedFloatingActionButton()
    }
}

@Composable
fun AnimatedItems(animateContentSize: Boolean) {
    var itemNum by remember { mutableStateOf(0) }
    Column {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { itemNum = min((itemNum + 1), 6) }) {
                Text("Add")
            }
            Button(onClick = { itemNum = max((itemNum - 1), 0) }) {
                Text("Remove")
            }
        }
        Box(
            Modifier.padding(bottom = 20.dp)
        ) {

            val modifier = if (animateContentSize) Modifier.animateContentSize() else Modifier
            Column(
                Modifier.background(Color.LightGray).fillMaxWidth().then(modifier)
            ) {

                Column(
                    Modifier.background(Color.LightGray).fillMaxWidth().then(modifier)
                ) {
                    AnimatedVisibility(visible = itemNum > 0) {
                        Item(
                            pastelColors[0],
                            "Expand Vertically + Fade In\nShrink " +
                                "Vertically + Fade Out\n(Column Default)"
                        )
                    }
                    HorizontalTransition(visible = itemNum > 1) {
                        Item(pastelColors[1], "Expand Horizontally\nShrink Horizontally")
                    }
                    SlideTransition(visible = itemNum > 2) {
                        Item(
                            pastelColors[2],
                            "Slide In Horizontally + Fade In\nSlide Out Horizontally + " +
                                "Fade Out"
                        )
                    }
                    AnimatedVisibility(
                        visible = itemNum > 3,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Item(pastelColors[3], "Expand Vertically\nShrink Vertically")
                    }
                    FadeTransition(visible = itemNum > 4) {
                        Item(pastelColors[4], "Fade In\nFade Out")
                    }
                    FullyLoadedTransition(visible = itemNum > 5) {
                        Item(
                            pastelColors[0],
                            "Expand Vertically + Fade In + Slide In Vertically\n" +
                                "Shrink Vertically + Fade Out + Slide Out Vertically"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Item(color: Color, text: String = "") {
    Box(Modifier.requiredHeight(80.dp).fillMaxWidth().background(color)) {
        Text(
            text,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)
        )
    }
}

@Composable
fun AnimateContentSizeOption(state: MutableState<Boolean>) {
    Row(
        Modifier.selectable(selected = state.value, onClick = { state.value = !state.value })
            .padding(10.dp)
    ) {
        Checkbox(state.value, { state.value = it })
        Text("AnimateContentSize", modifier = Modifier.align(Alignment.CenterVertically))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HorizontalTransition(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(
            // Set the start width to 20 (pixels), 0 by default
            initialWidth = { 20 }
        ),
        exit = shrinkHorizontally(
            // Shrink towards the end (i.e. right edge for LTR, left edge for RTL). The default
            // direction for the shrink is towards [Alignment.Start]
            shrinkTowards = Alignment.End,
            // Set the end width for the shrink animation to a quarter of the full width.
            targetWidth = { fullWidth -> fullWidth / 10 },
            // Overwrites the default animation with tween for this shrink animation.
            animationSpec = tween(durationMillis = 400)
        ) + fadeOut()
    ) {
        content()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SlideTransition(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            // Offsets the content by 1/3 of its width to the left, and slide towards right
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            // Overwrites the default animation with tween for this slide animation.
            animationSpec = tween(durationMillis = 200)
        ) + fadeIn(
            // Overwrites the default animation with tween
            animationSpec = tween(durationMillis = 200)
        ),
        exit = slideOutHorizontally(
            // Overwrites the ending position of the slide-out to 200 (pixels) to the right
            targetOffsetX = { 200 },
            animationSpec = spring(stiffness = Spring.StiffnessHigh)
        ) + fadeOut()
    ) {
        content()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FadeTransition(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            // Overwrites the initial value of alpha to 0.4f for fade in, 0 by default
            initialAlpha = 0.4f
        ),
        exit = fadeOut(
            // Overwrites the default animation with tween
            animationSpec = tween(durationMillis = 250)
        )
    ) {
        content()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FullyLoadedTransition(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            // Start the slide from 40 (pixels) above where the content is supposed to go, to
            // produce a parallax effect
            initialOffsetY = { -40 }
        ) + expandVertically(
            expandFrom = Alignment.Top
        ) + fadeIn(initialAlpha = 0.3f),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        content()
    }
}
