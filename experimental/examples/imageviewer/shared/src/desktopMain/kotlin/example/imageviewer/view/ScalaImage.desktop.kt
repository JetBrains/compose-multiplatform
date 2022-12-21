package example.imageviewer.view

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.addUserInput(state:MutableState<ScalableState>):Modifier =
    pointerInput(Unit) {
        detectDragGestures { change, dragAmount: Offset ->
            state.value = state.value.addDragAmount(dragAmount)
            change.consume()
        }
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val delta = event.changes.getOrNull(0)?.scrollDelta ?: Offset.Zero
                    state.value = state.value.addScale(delta.y / 100)
                }
            }
        }
    }.onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyUp) {
            when (it.key) {
                Key.I, Key.Plus, Key.Equals -> {
                    state.value = state.value.addScale(0.2f)
                }

                Key.O, Key.Minus -> {
                    state.value = state.value.addScale(-0.2f)
                }

                Key.R -> {
                    state.value = state.value.copy(scale = 1f)
                }
            }
        }
        false
    }
