package org.jetbrains.compose.demo.widgets.platform

import org.jetbrains.skiko.SystemTheme

actual fun isSystemInDarkTheme(): Boolean = org.jetbrains.skiko.currentSystemTheme == SystemTheme.DARK