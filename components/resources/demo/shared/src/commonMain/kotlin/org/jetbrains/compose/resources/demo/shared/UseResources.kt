package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import org.jetbrains.compose.resources.*

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun UseResources() {
    var textState by remember {  mutableStateOf("Hello, resources") }
    Column {
        Text(textState)
        Image(
            bitmap = resource("dir/img.png").rememberImageBitmap().orEmpty(),
            contentDescription = null,
        )
        Image(
            bitmap = resource("img.webp").rememberImageBitmap().orEmpty(),
            contentDescription = null,
        )
    }
    LaunchedEffect(Unit) {
        textState = resource("android.svg").readBytes().decodeToString()
    }
}
