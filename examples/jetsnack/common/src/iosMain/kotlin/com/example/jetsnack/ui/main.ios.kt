package com.example.jetsnack.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.example.jetsnack.JetSnackAppEntryPoint
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController =
    ComposeUIViewController {
        JetSnackAppEntryPoint()
    }