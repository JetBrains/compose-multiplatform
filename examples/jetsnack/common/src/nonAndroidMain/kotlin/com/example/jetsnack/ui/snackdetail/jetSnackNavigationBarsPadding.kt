package com.example.jetsnack.ui.snackdetail

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.jetSnackNavigationBarsPadding(): Modifier =
    this.windowInsetsPadding(WindowInsets.navigationBars)

@Composable
actual fun Modifier.jetSnackStatusBarsPadding(): Modifier =
    this.windowInsetsPadding(WindowInsets.statusBars)

@Composable
actual fun Modifier.jetSnackSystemBarsPadding(): Modifier =
    this.windowInsetsPadding(WindowInsets.systemBars)