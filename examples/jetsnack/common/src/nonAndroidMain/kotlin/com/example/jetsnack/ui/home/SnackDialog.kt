package com.example.jetsnack.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Popup

@Composable
actual fun SnackDialog(onCloseRequest: () -> Unit, content: @Composable () -> Unit) {
    Popup(onDismissRequest = onCloseRequest, content = { content() })
}