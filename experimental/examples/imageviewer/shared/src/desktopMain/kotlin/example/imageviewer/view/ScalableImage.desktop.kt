package example.imageviewer.view

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import example.imageviewer.model.ScalableState
import example.imageviewer.model.addDragAmount
import example.imageviewer.model.addScale

actual fun Modifier.addUserInput(state: ScalableState): Modifier =
    pointerInput(Unit) {
        detectDragGestures { change, dragAmount: Offset ->
            state.addDragAmount(dragAmount)
            change.consume()
        }
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val delta = event.changes.getOrNull(0)?.scrollDelta ?: Offset.Zero
                    state.addScale(delta.y / 100)
                }
            }
        }
    }
