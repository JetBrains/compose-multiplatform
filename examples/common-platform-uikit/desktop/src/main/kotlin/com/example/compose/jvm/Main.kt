/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example.compose.jvm

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.compose.common.DesktopApp

fun main() {
    application {
        Window(onCloseRequest = ::exitApplication) {
            MaterialTheme {
                DesktopApp()
            }
        }
    }
}
