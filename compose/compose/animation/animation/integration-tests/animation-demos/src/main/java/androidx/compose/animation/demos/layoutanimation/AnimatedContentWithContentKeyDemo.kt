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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedContentWithContentKeyDemo() {
    val model: ScreenModel = remember { ScreenModel() }
    val transition = updateTransition(model.target)
    Box {
        val holder = rememberSaveableStateHolder()
        transition.AnimatedContent(
            Modifier.clickable { model.toggleTarget() },
            contentAlignment = Alignment.Center,
            contentKey = { it.type }
        ) {
            if (it.type == MyScreen.Type.Count) {
                holder.SaveableStateProvider(it.type) {
                    var count by rememberSaveable { mutableStateOf(0) }
                    Column(
                        Modifier.fillMaxSize(),
                        Arrangement.Center,
                        Alignment.CenterHorizontally
                    ) {
                        Button(onClick = { count++ }) {
                            Text("+1")
                        }
                        Spacer(Modifier.size(20.dp))
                        Text("Count: $count", fontSize = 20.sp)
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().background(Color.LightGray))
            }
        }
        Text(
            "Tap anywhere to change content.\n Current content: ${model.target.type}",
            Modifier.align(Alignment.BottomCenter)
        )
    }
}

sealed class MyScreen {
    enum class Type { Count, Blank }

    abstract val type: Type
}

class CountScreen : MyScreen() {
    override val type = Type.Count
}

class BlankScreen : MyScreen() {
    override val type = Type.Blank
}

class ScreenModel {
    fun toggleTarget() {
        if (target.type == MyScreen.Type.Count) {
            target = BlankScreen()
        } else {
            target = CountScreen()
        }
    }

    var target: MyScreen by mutableStateOf(CountScreen())
        private set
}