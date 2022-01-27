package org.jetbrains.compose.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.singleWindowApplication

fun application(name: String = "application", title: String = "Compose Application", body: @Composable () -> Unit) {
    singleWindowApplication {
        body()
    }
}
