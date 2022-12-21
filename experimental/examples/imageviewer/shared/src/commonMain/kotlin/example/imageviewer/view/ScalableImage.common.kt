package example.imageviewer.view

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput


expect fun Modifier.addUserInput(state: MutableState<ScalableState>): Modifier


fun Modifier.addTouchUserInput(state: MutableState<ScalableState>): Modifier =
    pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            state.value = state.value.addDragAmount(pan).addScale(zoom - 1f)
        }
    }.pointerInput(Unit) {
        detectTapGestures(onDoubleTap = {
            state.value = state.value.copy(scale = 1f)
        })
    }
