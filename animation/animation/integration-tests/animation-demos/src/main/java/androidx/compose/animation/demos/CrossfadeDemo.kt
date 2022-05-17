/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Preview
@Composable
fun CrossfadeDemo() {
    var current by rememberSaveable { mutableStateOf(0) }
    Column {
        Row {
            tabs.forEachIndexed { index, tab ->
                Box(
                    Modifier.pointerInput(Unit) {
                        detectTapGestures {
                            current = index
                        }
                    }
                        .weight(1f, true)
                        .height(48.dp)
                        .background(tab.color)
                )
            }
        }
        val saveableStateHolder = rememberSaveableStateHolder()
        Crossfade(targetState = current, animationSpec = tween(1000)) { current ->
            saveableStateHolder.SaveableStateProvider(current) {
                val tab = tabs[current]
                arrayOf<Any?>()
                tab.lastInt = rememberSaveable { Random.nextInt() }
                Box(Modifier.fillMaxSize().background(tab.color))
            }
        }
    }
}

private val tabs = listOf(
    Tab(Color(1f, 0f, 0f)),
    Tab(Color(0f, 1f, 0f)),
    Tab(Color(0f, 0f, 1f))
)

private data class Tab(val color: Color) {
    var lastInt: Int = 0
        set(value) {
            if (value != field) {
                field = value
            }
        }
}