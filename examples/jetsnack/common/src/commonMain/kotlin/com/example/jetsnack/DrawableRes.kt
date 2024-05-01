package com.example.jetsnack

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
expect fun painterResource(id: Int): Painter

expect val MppR.drawable.empty_state_search: Int