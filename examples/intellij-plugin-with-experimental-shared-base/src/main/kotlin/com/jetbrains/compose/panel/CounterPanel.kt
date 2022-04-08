/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose.panel

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.*

@Composable
fun CounterPanel(mutableState: MutableState<CounterState>) {
    var state by remember { mutableState }
    Column {
        Text("Counter: ${state.counter}")
        Button(onClick = {
            state = state.copy(
                counter = state.counter + 1
            )
        }) {
            Text("Increment")
        }
    }
}
