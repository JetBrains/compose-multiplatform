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

package androidx.compose.runtime.saveable.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun SimpleNavigationWithSaveableStateSample() {
    @Composable
    fun <T : Any> Navigation(
        currentScreen: T,
        modifier: Modifier = Modifier,
        content: @Composable (T) -> Unit
    ) {
        // create SaveableStateHolder.
        val saveableStateHolder = rememberSaveableStateHolder()
        Box(modifier) {
            // Wrap the content representing the `currentScreen` inside `SaveableStateProvider`.
            // Here you can also add a screen switch animation like Crossfade where during the
            // animation multiple screens will be displayed at the same time.
            saveableStateHolder.SaveableStateProvider(currentScreen) {
                content(currentScreen)
            }
        }
    }

    Column {
        var screen by rememberSaveable { mutableStateOf("screen1") }
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { screen = "screen1" }) {
                Text("Go to screen1")
            }
            Button(onClick = { screen = "screen2" }) {
                Text("Go to screen2")
            }
        }
        Navigation(screen, Modifier.fillMaxSize()) { currentScreen ->
            if (currentScreen == "screen1") {
                Screen1()
            } else {
                Screen2()
            }
        }
    }
}

@Composable
fun Screen1() {
    var counter by rememberSaveable { mutableStateOf(0) }
    Button(onClick = { counter++ }) {
        Text("Counter=$counter on Screen1")
    }
}

@Composable
fun Screen2() {
    Text("Screen2")
}

@Composable
fun Button(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier
            .clickable(onClick = onClick)
            .background(Color(0xFF6200EE), RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        content()
    }
}