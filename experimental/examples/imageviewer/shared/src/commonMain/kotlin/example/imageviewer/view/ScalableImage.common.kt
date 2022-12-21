package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.*

@Composable
fun ScalableImage(image: ImageBitmap) {
    val focusRequester = FocusRequester()
    val imageSize = IntSize(image.width, image.height)
    val state = remember(imageSize) { mutableStateOf(ScalableState(imageSize)) }

    Box(
        modifier = Modifier.fillMaxSize()
            .onGloballyPositioned { coordinates ->
                state.value = state.value.changeBoxSize(coordinates.size)
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().background(Color.DarkGray)
                .addUserInput(state)
                .focusRequester(focusRequester)
                .focusable()
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = BitmapPainter(
                    image,
                    srcOffset = state.value.visiblePart.topLeft,
                    srcSize = state.value.visiblePart.size
                ),
                contentDescription = null
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

expect fun Modifier.addUserInput(state:MutableState<ScalableState>):Modifier
