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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * These demos are for using the memory profiler to observe initial compo and recompo memory
 * pressure.
 *
 * Emulate recompose when string loads
 */
@Composable
fun MemoryAllocsSetText() {
    Column {
        Preamble("""
            @Composable
            fun SetText(text: State<String>) {
                Text(text.value)
            }""".trimIndent()
        )
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
        Preamble("""
            @Composable
            fun IfNotEmptyText(text: State<String>) {
                if (text.value.isNotEmpty()) {
                    Text(text.value)
                }
            }""".trimIndent()
        )
        IfNotEmptyText(textToggler())
    }
}

@Composable
fun Preamble(sourceCode: String) {
    Text("Run in memory profiler to emulate text behavior during observable loads")
    Text(text = sourceCode,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(220, 230, 240)),
        fontFamily = FontFamily.Monospace,
        color = Color(41, 17, 27),
        fontSize = 10.sp
    )
    Divider(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp))
    Text("\uD83D\uDC47 running here \uD83D\uDC47")
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
