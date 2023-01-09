/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Application
import platform.UIKit.UIViewController

fun MainViewController() : UIViewController =
    Application("Minesweeper") {
        Game()
    }


@Composable
internal actual fun loadImage(src: String): Painter = loadImageAsColoredRect(src)

actual fun isMobileDevice() = true