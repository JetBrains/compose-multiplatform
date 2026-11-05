/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */


import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import org.jetbrains.codeviewer.ui.MainView

fun MainViewController() : UIViewController = ComposeUIViewController { MainView() }
