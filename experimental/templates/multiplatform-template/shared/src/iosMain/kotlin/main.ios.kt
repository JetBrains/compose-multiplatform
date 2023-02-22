/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.ui.window.Application
import platform.UIKit.UIViewController

actual fun getPlatformName(): String = "iOS"

fun MainViewController() : UIViewController =
    Application("Demo") {
        App()
    }
