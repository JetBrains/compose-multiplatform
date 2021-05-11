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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * This demo shows how to create a Transition<EnterExitState> with MutableTransitionState, and
 * use the Transition<EnterExitState> to animate a few enter/exit transitions together. The
 * MutableTransitionState is then used to add sequential enter/exit transitions.
 *
 *  APIs used:
 * - updateTransition
 * - MutableTransitionState
 * - EnterExitState
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalTransitionApi::class)
@Composable
fun SequentialEnterExitDemo() {
    Box {
        var mainContentVisible by remember {
            mutableStateOf(MutableTransitionState(true))
        }
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.size(40.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // New MutableTransitionState instance here. This should reset the animation.
                Button(onClick = { mainContentVisible = MutableTransitionState(false) }) {
                    Text("Reset")
                }

                Button(
                    onClick = { mainContentVisible.targetState = !mainContentVisible.targetState },
                ) {
                    Text("Toggle Visibility")
                }
            }
            Spacer(Modifier.size(40.dp))
            AnimatedVisibility(
                visibleState = mainContentVisible,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box {
                    Column(Modifier.fillMaxSize()) {
                        Item(Modifier.weight(1f), backgroundColor = Color(0xffff6f69))
                        Item(Modifier.weight(1f), backgroundColor = Color(0xffffcc5c))
                    }
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
        AnimatedVisibility(
            visible = mainContentVisible.targetState && mainContentVisible.isIdle,
            modifier = Modifier.align(Alignment.Center),
            enter = expandVertically(),
            exit = fadeOut(animationSpec = tween(50))
        ) {
            Text("Transition Finished", color = Color.White, fontSize = 40.sp, fontWeight = Bold)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedVisibilityScope.Item(
    modifier: Modifier,
    backgroundColor: Color
) {
    val scale by transition.animateFloat { enterExitState ->
        when (enterExitState) {
            EnterExitState.PreEnter -> 0.9f
            EnterExitState.Visible -> 1.0f
            EnterExitState.PostExit -> 0.5f
        }
    }
    Box(
        modifier.fillMaxWidth().padding(5.dp).animateEnterExit(
            enter = slideInVertically(initialOffsetY = { it }),
            exit = ExitTransition.None
        ).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.clip(RoundedCornerShape(20.dp)).background(backgroundColor).fillMaxSize()
    ) {}
}
