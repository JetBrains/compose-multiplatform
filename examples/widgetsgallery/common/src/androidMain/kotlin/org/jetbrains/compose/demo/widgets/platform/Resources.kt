package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.demo.widgets.R

@Composable
actual fun imageResource(res: String): ImageBitmap {
    val id = drawableId(res)
    return androidx.compose.ui.res.imageResource(id)
}

@Composable
actual fun vectorResource(res: String): ImageVector {
    val id = drawableId(res)
    return androidx.compose.ui.res.vectorResource(id)
}

// TODO: improve resource loading
private fun drawableId(res: String): Int {
    val imageName = res.substringAfterLast("/").substringBeforeLast(".")
    val drawableClass = R.drawable::class.java
    val field = drawableClass.getDeclaredField(imageName)
    val idValue = field.get(drawableClass) as Integer
    return idValue.toInt()
}