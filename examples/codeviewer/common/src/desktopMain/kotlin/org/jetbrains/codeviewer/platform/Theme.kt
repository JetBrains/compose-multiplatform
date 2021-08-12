package org.jetbrains.codeviewer.platform

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.runtime.Composable

@Composable
actual fun PlatformTheme(content: @Composable () -> Unit) = DesktopMaterialTheme(content = content)