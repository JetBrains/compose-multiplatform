package example.imageviewer.view

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import example.imageviewer.model.ScalableState
import example.imageviewer.model.addDragAmount
import example.imageviewer.model.addScale
import example.imageviewer.model.setScale

expect fun Modifier.addUserInput(state: MutableState<ScalableState>): Modifier

fun Modifier.addTouchUserInput(state: MutableState<ScalableState>): Modifier =
    pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            state.addDragAmount(pan)
            state.addScale(zoom - 1f)
        }
    }.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { state.setScale(1f) }
        )
    }
