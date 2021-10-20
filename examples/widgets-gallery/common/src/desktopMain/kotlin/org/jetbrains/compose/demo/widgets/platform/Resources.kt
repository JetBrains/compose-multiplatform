package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.loadXmlImageVector
import androidx.compose.ui.res.painterResource

@Composable
actual fun imageResource(res: String): Painter = painterResource(res)

@Composable
actual fun vectorResource(res: String): Painter = painterResource(res)