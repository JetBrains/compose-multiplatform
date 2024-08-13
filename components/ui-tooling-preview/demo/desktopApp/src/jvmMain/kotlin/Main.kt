/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.preview.demo.shared.MainView

fun main() =
    singleWindowApplication(
        title = "Preview annotation demo",
        state = WindowState(size = DpSize(800.dp, 800.dp))
    ) {
        MainView()
    }
