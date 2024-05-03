package com.example.jetsnack.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
actual fun snackCollectionListItemWindowInsets(): WindowInsets {
    return WindowInsets.statusBars.add(WindowInsets(top = 56.dp))
}