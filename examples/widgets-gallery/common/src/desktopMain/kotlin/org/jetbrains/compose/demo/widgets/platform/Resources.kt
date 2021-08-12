package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorXmlResource
import androidx.compose.ui.res.imageResource as bitmapImage

@Composable
actual fun imageResource(res: String) =
    bitmapImage(res)

@Composable
actual fun vectorResource(res: String): ImageVector =
    vectorXmlResource(res)