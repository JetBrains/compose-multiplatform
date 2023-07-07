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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun CraneDemo() {
    val avatar = remember {
        movableContentWithReceiverOf<SceneScope> {
            Box(
                Modifier
                    .sharedElement()
                    .background(Color(0xffff6f69), RoundedCornerShape(20))
                    .fillMaxSize()
            )
        }
    }

    val parent = remember {
        movableContentWithReceiverOf<SceneScope, @Composable () -> Unit> { child ->
            Surface(
                modifier = Modifier
                    .sharedElement()
                    .background(Color(0xfffdedac)),
                color = Color(0xfffdedac),
                shape = RoundedCornerShape(10.dp)
            ) {
                child()
            }
        }
    }

    var fullScreen by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .padding(10.dp)
            .clickable { fullScreen = !fullScreen },
        contentAlignment = Alignment.Center
    ) {
        SceneHost(Modifier.fillMaxSize()) {
            if (fullScreen) {
                Box(Modifier.offset(100.dp, 150.dp)) {
                    parent {
                        Box(
                            Modifier
                                .padding(10.dp)
                                .wrapContentSize(Alignment.Center)
                                .size(50.dp)
                        ) {
                            avatar()
                        }
                    }
                }
            } else {
                parent {
                    Column(Modifier.fillMaxSize()) {
                        val alpha = produceState(0f) {
                            animate(0f, 1f, animationSpec = tween(200)) { value, _ ->
                                this.value = value
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .graphicsLayer {
                                    this.alpha = alpha.value
                                }
                                .background(Color.DarkGray)
                                .animateContentSize())
                        Box(
                            Modifier
                                .padding(10.dp)
                                .size(60.dp)
                        ) {
                            avatar()
                        }
                    }
                }
            }
        }
    }
}
