/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis

/**
 * These demos are for using the memory profiler to observe initial compo and recompo memory
 * pressure.
 *
 * Emulate recompose when string loads
 */
@Composable
fun MemoryAllocsSetText() {
    Column {
        Text("Run in memory profiler to emulate setting text value when observable loads")
        Text("This is designed to be used in the Android Studio memory profiler")
        SetText(textToggler())
    }
}

/**
 * These demos are for using the memory profiler to observe initial compo and recompo memory
 * pressure.
 *
 * Emulate calling text when string loads
 */
@Composable
fun MemoryAllocsIfNotEmptyText() {
    Column {
        Text("Run in memory profiler to emulate calling Text after an observable loads")
        Text("This is designed to be used in the Android Studio memory profiler")
        IfNotEmptyText(textToggler())
    }
}

@Composable
fun IfNotEmptyText(text: State<String>) {
    if (text.value.isNotEmpty()) {
        Text(text.value)
    }
}

@Composable
private fun SetText(text: State<String>) {
    Text(text.value)
}

@Composable
private fun textToggler(): State<String> = produceState("") {
    while (true) {
        withFrameMillis {
            value = if (value.isEmpty()) {
                "This text and empty string swap every frame"
            } else {
                ""
            }
        }
    }
}
