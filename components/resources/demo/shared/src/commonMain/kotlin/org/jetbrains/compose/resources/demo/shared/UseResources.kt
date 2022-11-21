package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.*

val mutableStateFlow = MutableStateFlow(true)
var recompositionCount = 0

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun UseResources() {
    Column {
        Text("Hello, resources")
        Image(
            bitmap = resource("dir/img.png").rememberImageBitmap(),
            contentDescription = null,
        )

        val state: Boolean by mutableStateFlow.collectAsState(true)
        val resource = resource(if (state) "1.png" else "2.png")
        CountRecompositions(resource.rememberImageBitmapAsync()) {
            recompositionCount++
        }

        LaunchedEffect(Unit) {
            delay(1000)
            mutableStateFlow.value = false
            delay(1000)
            mutableStateFlow.value = true
            delay(100)
        }
    }
}

@Composable
fun CountRecompositions(imageBitmap: ImageBitmap?, onRecomposition: () -> Unit) {
    onRecomposition()
    println("imageBitmap: $imageBitmap")
    if (imageBitmap != null) {
        Image(bitmap = imageBitmap, contentDescription = null)
    }
}
