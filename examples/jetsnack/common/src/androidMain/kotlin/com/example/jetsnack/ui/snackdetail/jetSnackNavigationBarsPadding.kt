package com.example.jetsnack.ui.snackdetail

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier

actual fun Modifier.jetSnackNavigationBarsPadding(): Modifier = this.navigationBarsPadding()
actual fun Modifier.jetSnackStatusBarsPadding(): Modifier = this.statusBarsPadding()
actual fun Modifier.jetSnackSystemBarsPadding(): Modifier = this.systemBarsPadding()