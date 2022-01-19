import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*

@Composable
fun Modifier.gameInteraction(open: () -> Unit, flag: () -> Unit, seek: () -> Unit): Modifier =
    pointerInput(open, flag, seek) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                with(event) {
                    if (type == PointerEventType.Press) {
                        // TODO does not work yet, all events are of Unknown type (
                        val lmb = buttons.isPrimaryPressed
                        val rmb = buttons.isSecondaryPressed

                        if (lmb && !rmb) {
                            if (keyboardModifiers.isShiftPressed) {
                                seek()
                            } else {
                                open()
                            }
                        } else if (rmb && !lmb) {
                            flag()
                        }
                    }
                }
            }
        }
    }