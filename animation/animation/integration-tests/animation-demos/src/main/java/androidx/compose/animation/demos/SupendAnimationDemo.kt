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

package androidx.compose.animation.demos

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.isFinished
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPointerInput::class)
@Composable
fun SuspendAnimationDemo() {
    var animStateX by remember {
        mutableStateOf(AnimationState(0f))
    }
    var animStateY by remember {
        mutableStateOf(AnimationState(0f))
    }
    val mutex = remember { MutatorMutex() }

    Box(
        Modifier.fillMaxSize().background(Color(0xffb99aff)).pointerInput {
            coroutineScope {
                while (true) {
                    val offset = handlePointerInput {
                        awaitFirstDown().current.position
                    }
                    if (offset != null) {
                        val x = offset.x
                        val y = offset.y
                        mutex.mutate {
                            launch {
                                animStateX.animateTo(
                                    x,
                                    sequentialAnimation = !animStateX.isFinished,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                                )
                            }
                            launch {
                                animStateY.animateTo(
                                    y,
                                    sequentialAnimation = !animStateY.isFinished,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Text("Tap anywhere", Modifier.align(Alignment.Center))
        Box(
            Modifier.offset({ animStateX.value }, { animStateY.value }).size(40.dp)
                .background(Color(0xff3c1361), CircleShape)
        )
    }
}
