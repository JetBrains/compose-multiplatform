package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme as androidSystemIsInDarkTheme

@Composable
actual fun isSystemInDarkTheme(): Boolean =
    androidSystemIsInDarkTheme()