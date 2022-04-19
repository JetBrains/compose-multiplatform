package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
expect fun painterResource(res: String): Painter
