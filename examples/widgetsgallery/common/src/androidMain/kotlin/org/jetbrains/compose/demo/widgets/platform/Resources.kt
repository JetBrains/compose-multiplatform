package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AmbientContext

@Composable
actual fun imageResource(res: String): ImageBitmap {
    val context = AmbientContext.current
    val id = context.resources.getIdentifier(res, "drawable", context.packageName)
    return androidx.compose.ui.res.imageResource(id)
}

@Composable
actual fun vectorResource(res: String): ImageVector {
    val context = AmbientContext.current
    val id = context.resources.getIdentifier(res, "drawable", context.packageName)
    return androidx.compose.ui.res.vectorResource(id)
}