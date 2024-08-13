package com.example.jetsnack.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog

@Composable
actual fun SnackDialog(onCloseRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onCloseRequest, content = content)
}