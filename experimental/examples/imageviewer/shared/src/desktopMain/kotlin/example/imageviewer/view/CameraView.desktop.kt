package example.imageviewer.view

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.utils.rememberWebcamListState
import example.imageviewer.utils.rememberWebcamState

@Composable
internal actual fun CameraView(modifier: Modifier) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val webcamListState = rememberWebcamListState()
        if (webcamListState.isLoading.not()) {
            if (webcamListState.webcams.isNotEmpty()) {
                val webcamState = rememberWebcamState(webcamListState.defaultWebcam!!)

                val lastFrame = webcamState.lastFrame
                if (lastFrame != null) {
                    Image(
                        modifier = Modifier.matchParentSize(),
                        bitmap = lastFrame,
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.High
                    )
                } else {
                    CameraText(text = "Camera buffer loading.")
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                ) {
                    if(lastFrame != null) {
                        Button(
                            onClick = {
                                //todo pass image to gallery page
                            },
                        ) {
                            Text("Take a photo")
                        }
                        Spacer(Modifier.padding(horizontal = 8.dp))
                    }
                    EasyDropdown(
                        selectedItem = webcamState.webcam,
                        items = webcamListState.webcams,
                        itemName = { webcam -> webcam.name },
                        onSelected = { webcam -> webcamState.webcam = webcam }
                    )
                }
            } else {
                CameraText(text = "Camera not found, connect one.")
            }
        } else {
            CameraText(text = "Camera is loading.")
        }

    }
}

@Composable
private fun BoxScope.CameraText(text: String) {
    Text(
        text = text,
        color = Color.White,
        modifier = Modifier.align(Alignment.Center)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun <T> EasyDropdown(
    selectedItem: T,
    items: List<T>,
    itemName: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    val state = remember { ContextMenuState() }
    Button(
        onClick = { isOpen = true },
        modifier = modifier.onPointerEvent(PointerEventType.Press) {
            state.status =
                ContextMenuState.Status.Open(Rect(it.changes[0].position, 0f))
        },
    ) {
        val displayName = remember(selectedItem) { itemName(selectedItem) }
        Text(displayName)
    }
    val items = remember(items) {
        items.map { ContextMenuItem(itemName(it), { onSelected(it) }) }
    }

    ContextMenuArea( { items }, state) {}
}