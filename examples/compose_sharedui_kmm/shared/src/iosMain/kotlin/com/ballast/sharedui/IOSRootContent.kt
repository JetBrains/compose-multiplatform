package com.ballast.sharedui

import androidx.compose.ui.window.ComposeUIViewController
import com.ballast.sharedui.content.RootContent
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused") // Used in iOS
fun RootViewController(): UIViewController = ComposeUIViewController {
    RootContent()
}
