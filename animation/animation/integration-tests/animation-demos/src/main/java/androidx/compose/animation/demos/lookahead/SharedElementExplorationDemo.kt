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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SharedElementExplorationDemo() {
    val A = remember {
        movableContentWithReceiverOf<SceneScope, Modifier> { modifier ->
            Box(
                modifier = Modifier
                    .sharedElement().then(modifier)
                    .background(color = Color(0xfff3722c), RoundedCornerShape(10))
            )
        }
    }
    val B = remember {
        movableContentWithReceiverOf<SceneScope, Modifier> { modifier ->
            Box(
                modifier = Modifier
                    .sharedElement().then(modifier)
                    .background(color = Color(0xff90be6d), RoundedCornerShape(10))
            )
        }
    }

    val C = remember {
        movableContentWithReceiverOf<SceneScope, @Composable () -> Unit> { content ->
            Box(Modifier.sharedElement().background(Color(0xfff9c74f)).padding(20.dp)) {
                content()
            }
        }
    }

    var isHorizontal by remember { mutableStateOf(true) }

    SceneHost(Modifier.fillMaxSize().clickable { isHorizontal = !isHorizontal }) {
        Box(contentAlignment = Alignment.Center) {
            if (isHorizontal) {
                C {
                    Row(Modifier.background(Color.Gray).padding(10.dp)) {
                        A(Modifier.size(40.dp))
                        B(Modifier.size(40.dp))
                        Box(Modifier.size(40.dp).background(Color(0xff4d908e)))
                    }
                }
            } else {
                C {
                    Column(Modifier.background(Color.DarkGray).padding(10.dp)) {
                        A(Modifier.size(width = 120.dp, height = 60.dp))
                        B(Modifier.size(width = 120.dp, height = 60.dp))
                    }
                }
            }
        }
    }
}