package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.Composable
import org.jetbrains.skiko.SystemTheme

@Composable
internal actual fun isSystemInDarkTheme(): Boolean = org.jetbrains.skiko.currentSystemTheme == SystemTheme.DARK
