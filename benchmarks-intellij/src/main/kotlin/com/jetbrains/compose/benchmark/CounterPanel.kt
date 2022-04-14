/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose.benchmark

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.*

@Composable
fun CounterPanel(stateWithIdeLifecycle: MutableState<CounterState>) {
    var stateInline by remember { mutableStateOf(CounterState()) }
    Column {
        Text("Counter with IDE lifecycle: ${stateWithIdeLifecycle.value.counter}")
        Button(onClick = {
            stateWithIdeLifecycle.value = stateWithIdeLifecycle.value.copy(
                counter = stateWithIdeLifecycle.value.counter + 1
            )
        }) {
            Text("Increment state with IDE lifecycle")
        }
        Text("Counter with @Composable lifecycle: ${stateInline.counter}")
        Button(onClick = {
            stateInline = stateInline.copy(
                counter = stateInline.counter + 1
            )
        }) {
            Text("Increment state with @Composable lifecycle")
        }
    }
}
