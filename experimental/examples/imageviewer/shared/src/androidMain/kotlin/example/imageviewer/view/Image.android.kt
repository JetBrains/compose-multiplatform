package example.imageviewer.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import example.imageviewer.model.ContentState
import example.imageviewer.style.DarkGray
import example.imageviewer.utils.adjustImageScale

@Composable
internal actual fun Image(content: ContentState) {
    val drag = remember { DragHandler() }
    val scaleHandler = remember { ScaleHandler(content.state) }

    Surface(
        color = DarkGray,
        modifier = Modifier.fillMaxSize()
    ) {
        Draggable(dragHandler = drag, modifier = Modifier.fillMaxSize()) {
            Scalable(onScale = scaleHandler, modifier = Modifier.fillMaxSize()) {
                val bitmap = imageByGesture(content, scaleHandler, drag)
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = adjustImageScale(bitmap)
                )
            }
        }
    }
}
