/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.ui.window.ComposeUIViewController
import org.jetbrains.compose.demo.widgets.ui.MainView
import platform.UIKit.UIViewController

fun MainViewController() : UIViewController = ComposeUIViewController { MainView() }