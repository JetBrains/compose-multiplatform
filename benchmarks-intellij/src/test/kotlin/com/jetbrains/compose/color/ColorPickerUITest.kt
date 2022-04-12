/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose.color

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    val windowState = remember { WindowState(width = 400.dp, height = 400.dp) }
    Window(
        onCloseRequest = ::exitApplication,
        title = "ColorPicker",
        state = windowState
    ) {
        ColorPicker(mutableStateOf(Color(0xffaabbcc)))
    }
}
