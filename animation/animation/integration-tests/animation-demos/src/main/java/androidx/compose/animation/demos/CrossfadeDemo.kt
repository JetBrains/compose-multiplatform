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

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.savedinstancestate.ExperimentalRestorableStateHolder
import androidx.compose.runtime.savedinstancestate.rememberRestorableStateHolder
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@OptIn(ExperimentalRestorableStateHolder::class)
@Composable
fun CrossfadeDemo() {
    var current by savedInstanceState { 0 }
    Column {
        Row {
            tabs.forEachIndexed { index, tab ->
                Box(
                    Modifier.pointerInput {
                        detectTapGestures {
                            Log.e("Crossfade", "Switch to $tab")
                            current = index
                        }
                    }
                        .weight(1f, true)
                        .preferredHeight(48.dp)
                        .background(tab.color)
                )
            }
        }
        val restorableStateHolder = rememberRestorableStateHolder<Int>()
        Crossfade(current = current) { current ->
            restorableStateHolder.RestorableStateProvider(current) {
                val tab = tabs[current]
                tab.lastInt = rememberSavedInstanceState { Random.nextInt() }
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
                Log.e("Crossfade", "State recreated for $color")
                field = value
            }
        }
}