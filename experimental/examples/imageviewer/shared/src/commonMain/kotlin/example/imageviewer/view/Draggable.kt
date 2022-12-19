package example.imageviewer.view

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import example.imageviewer.core.EventLocker
import example.imageviewer.style.Transparent

@Composable
internal fun Draggable(
    dragHandler: DragHandler,
    modifier: Modifier = Modifier,
    onUpdate: (() -> Unit)? = null,
    children: @Composable() () -> Unit
) {
    Surface(
        color = Transparent,
        modifier = modifier.run {
            if (false) {//todo
                pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragHandler.reset() },
                        onDragEnd = { dragHandler.reset() },
                        onDragCancel = { dragHandler.cancel() },
                    ) { change, dragAmount ->
                        dragHandler.drag(dragAmount)
                        onUpdate?.invoke()
                        change.consume()
                    }
                }

            } else {
                this
            }
        }
    ) {
        children()
    }
}

class DragHandler {//todo check drag

    private val amount = mutableStateOf(Offset(0f, 0f))
    private val distance = mutableStateOf(Offset(0f, 0f))
    private val locker: EventLocker = EventLocker()

    fun getAmount(): Offset {
        return amount.value
    }

    fun getDistance(): Offset {
        return distance.value
    }

    fun reset() {
        distance.value = Offset.Zero
        locker.unlock()
    }

    fun cancel() {
        distance.value = Offset.Zero
        locker.lock()
    }

    fun drag(dragDistance: Offset) {
        if (locker.isLocked()) {
            val dx = dragDistance.x
            val dy = dragDistance.y

            distance.value = Offset(distance.value.x + dx, distance.value.y + dy)
            amount.value = Offset(amount.value.x + dx, amount.value.y + dy)
        }
    }
}
