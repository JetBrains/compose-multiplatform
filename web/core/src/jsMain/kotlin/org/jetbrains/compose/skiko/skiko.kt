/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.skiko

import org.jetbrains.compose.web.dom.ElementScope
import org.w3c.dom.HTMLCanvasElement
import org.jetbrains.skiko.wasm.onWasmReady
import androidx.compose.ui.window.Window
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

fun ElementScope<HTMLCanvasElement>.ping() {
    onWasmReady {
        Window("whatevs") {
            var switched by remember { mutableStateOf(false) }
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    println("Button clicked!")
                    switched = !switched
                }
            ) {
                Text(if (switched) "🦑 press 🐙" else "Press me!")
            }
        }
    }
}