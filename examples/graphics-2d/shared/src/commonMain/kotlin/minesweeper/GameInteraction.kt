package minesweeper

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.gameInteraction(open: () -> Unit, flag: () -> Unit, seek: () -> Unit): Modifier =
    if (!hasRightClick()) {
        combinedClickable(
            onClick = {
                open()
            },
            onDoubleClick = {
                seek()
            },
            onLongClick = {
                flag()
            }
        )
    } else {
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
    }
