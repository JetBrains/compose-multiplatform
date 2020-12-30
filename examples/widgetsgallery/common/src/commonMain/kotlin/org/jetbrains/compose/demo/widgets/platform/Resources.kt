package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
expect fun imageResource(res: String): ImageBitmap

@Composable
expect fun vectorResource(res: String): ImageVector

