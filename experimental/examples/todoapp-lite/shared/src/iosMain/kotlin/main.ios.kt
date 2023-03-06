/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import example.todoapp.lite.common.RootContent
import platform.UIKit.UIViewController

fun MainViewController() : UIViewController = ComposeUIViewController {
    RootContent(modifier = Modifier.fillMaxSize())
}