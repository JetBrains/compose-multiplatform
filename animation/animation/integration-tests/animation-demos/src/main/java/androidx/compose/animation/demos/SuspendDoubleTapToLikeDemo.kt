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

import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Preview
@Composable
fun SuspendDoubleTapToLikeDemo() {
    var alpha by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(0f) }

    val mutatorMutex = MutatorMutex()
    val scope = rememberCoroutineScope()
    Box(
        Modifier.fillMaxSize().background(Color.White).pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    scope.launch {
                        // MutatorMutex cancels the previous job (i.e. animations) before starting
                        // a new job. This ensures the mutable states only being mutated by one
                        // animation at a time.
                        mutatorMutex.mutate {
                            coroutineScope {
                                // `launch` creates a new coroutine without blocking. This allows
                                // the two animations in this CoroutineScope to run together.
                                launch {
                                    animate(0f, 1f) { value, _ ->
                                        alpha = value
                                    }
                                }
                                launch {
                                    animate(0f, 2f) { value, _ ->
                                        scale = value
                                    }
                                }
                            }
                            // CoroutineScope doesn't return until all animations in the scope
                            // finish. So by the time we get here, the enter animations from the
                            // previous CoroutineScope have all finished.
                            coroutineScope {
                                launch {
                                    animate(alpha, 0f) { value, _ ->
                                        alpha = value
                                    }
                                }
                                launch {
                                    animate(scale, 4f) { value, _ ->
                                        scale = value
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    ) {
        Icon(
            Icons.Filled.Favorite,
            "Like",
            Modifier.align(Alignment.Center)
                .graphicsLayer(
                    alpha = alpha,
                    scaleX = scale,
                    scaleY = scale
                ),
            tint = Color.Red
        )
    }
}
