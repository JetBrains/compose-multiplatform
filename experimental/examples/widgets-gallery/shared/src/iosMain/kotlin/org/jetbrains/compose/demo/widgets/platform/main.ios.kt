/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Application
import org.jetbrains.compose.demo.widgets.ui.MainView
import platform.UIKit.UIViewController

fun MainViewController() : UIViewController =
    Application("WidgetsGallery") {
        Column {
            // To skip upper part of screen.
            Box(
                modifier = Modifier
                    .height(30.dp)
            )
            MainView()
        }
    }
