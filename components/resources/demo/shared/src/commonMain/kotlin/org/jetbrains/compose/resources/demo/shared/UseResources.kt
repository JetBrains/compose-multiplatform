package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.*

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun UseResources() {
    Column {
        Text("Hello, resources")
        Image(
            bitmap = resource("dir/img.png").rememberImageBitmapAsync(),
            contentDescription = null,
        )
        Image(
            bitmap = resource("img.webp").rememberImageBitmap(),
            contentDescription = null,
        )
    }
}
