package com.example.jetsnack.ui.snackdetail

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.jetSnackNavigationBarsPadding(): Modifier = this.navigationBarsPadding()
@Composable
actual fun Modifier.jetSnackStatusBarsPadding(): Modifier = this.statusBarsPadding()
@Composable
actual fun Modifier.jetSnackSystemBarsPadding(): Modifier = this.systemBarsPadding()