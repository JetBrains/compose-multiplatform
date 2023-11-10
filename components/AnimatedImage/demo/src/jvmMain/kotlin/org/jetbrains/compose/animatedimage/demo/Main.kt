package org.jetbrains.compose.animatedimage.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.animatedimage.AnimatedImage
import org.jetbrains.compose.animatedimage.Blank
import org.jetbrains.compose.animatedimage.animate
import org.jetbrains.compose.animatedimage.loadAnimatedImage

private val url =
    "https://raw.githubusercontent.com/JetBrains/skija/ccf303ebcf926e5ef000fc42d1a6b5b7f1e0b2b5/examples/scenes/images/codecs/animated.gif"

private sealed interface LoadState<T> {
    class Loading<T> : LoadState<T>
    data class Success<T>(val data: T) : LoadState<T>
    data class Error<T>(val error: Exception) : LoadState<T>
}

@Composable
private fun <T> loadOrNull(action: suspend () -> T?): T? {
    val scope = rememberCoroutineScope()
    var result: T? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        result = action()
    }
    return result
}

fun main() = singleWindowApplication {
    Column {
        var state: LoadState<AnimatedImage> = LoadState.Loading()
        LaunchedEffect(url) {
            state = try {
                LoadState.Success(loadAnimatedImage(url))
            } catch (e: Exception) {
                LoadState.Error(e)
            }
        }

        when (val animatedImage = state) {
            is LoadState.Success -> Image(
                bitmap = animatedImage.data.animate(),
                contentDescription = null,
            )

            is LoadState.Loading -> CircularProgressIndicator()
            is LoadState.Error -> Text("Error!")
        }

        Image(
            loadOrNull { loadAnimatedImage(url) }?.animate() ?: ImageBitmap.Blank,
            contentDescription = null,
            Modifier.size(100.dp)
        )
    }
}