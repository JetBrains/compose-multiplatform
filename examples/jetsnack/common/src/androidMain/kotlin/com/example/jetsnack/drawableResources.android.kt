package com.example.jetsnack

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
actual fun painterResource(id: Int): Painter {
    return androidx.compose.ui.res.painterResource(id)
}

actual val MppR.drawable.empty_state_search: Int
    get() = R.drawable.empty_state_search

