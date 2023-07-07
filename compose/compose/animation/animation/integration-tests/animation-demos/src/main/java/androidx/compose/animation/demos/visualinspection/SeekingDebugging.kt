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

package androidx.compose.animation.demos.visualinspection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Preview
@Composable
fun SeekingDemo() {
    val transition = updateTransition(true)

    var entering by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }
    Column(
        Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(Modifier.clickable { entering = true }.padding(10.dp)) {
                RadioButton(entering, { entering = true })
                Text("Entering")
            }

            Row(Modifier.clickable { entering = false }.padding(10.dp)) {
                RadioButton(!entering, { entering = false })
                Text("Exiting")
            }
        }
        Slider(progress, { progress = it })
        transition.ComplexAV()
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            progress * if (entering) 1f else -1f
        }.collect {
            val totalDuration = transition.totalDurationNanos
            if (entering) {
                transition.setPlaytimeAfterInitialAndTargetStateEstablished(
                    false, true, (abs(it) * totalDuration).toLong()
                )
            } else {
                transition.setPlaytimeAfterInitialAndTargetStateEstablished(
                    true, false, (abs(it) * totalDuration).toLong()
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Transition<Boolean>.ComplexAV() {
    AnimatedVisibility(
        visible = { it },
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box {
            Column(Modifier.fillMaxSize()) {
                colors.forEachIndexed { index, color ->
                    // Creates a custom enter/exit animation on scale using
                    // `AnimatedVisibilityScope.transition`
                    val scale by transition.animateFloat { enterExitState ->
                        when (enterExitState) {
                            EnterExitState.PreEnter -> 0.9f
                            EnterExitState.Visible -> 1.0f
                            EnterExitState.PostExit -> 0.5f
                        }
                    }
                    val staggeredSpring = remember {
                        spring<IntOffset>(
                            stiffness = Spring.StiffnessLow * (1f - index * 0.2f)
                        )
                    }
                    Box(
                        Modifier.weight(1f).animateEnterExit(
                            // Staggered slide-in from bottom, while the parent
                            // AnimatedVisibility fades in everything (including this child)
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = staggeredSpring
                            ),
                            // No built-in exit transition will be applied. It'll be
                            // faded out by parent AnimatedVisibility while scaling down
                            // by the scale animation.
                            exit = ExitTransition.None
                        ).fillMaxWidth().padding(5.dp).graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }.clip(RoundedCornerShape(20.dp)).background(color)
                    ) {}
                }
            }
            // This gets faded in/out by the parent AnimatedVisibility
            FloatingActionButton(
                onClick = {},
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null)
            }
        }
    }
}

private val colors = listOf(
    Color(0xffff6f69),
    Color(0xffffcc5c),
    Color(0xff2a9d84),
    Color(0xff264653)
)
