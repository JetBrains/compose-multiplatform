/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.ui.window.Application
import platform.UIKit.UIViewController

fun MainViewController() : UIViewController =
    Application("Minesweeper") {
        Game()
    }


actual fun hasRightClick() = false