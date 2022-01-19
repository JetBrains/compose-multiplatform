import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        Window("Minesweeper") {
            Game(
                requestWindowSize = { w, h ->
                    // TODO(not implemented yet)
                }
            )
        }
    }
}


@Composable
actual fun CellWithIcon(src: String, alt: String) {
    Image(
        painter = painterResource(src),
        contentDescription = alt,
        modifier = Modifier.fillMaxSize().padding(Dp(4.0f))
    )
}

fun painterResource(src: String): Painter {
    return object : Painter() {
        override val intrinsicSize: Size
            get() = Size(16f, 16f)

        override fun DrawScope.onDraw() {
            drawRect(color = Color.Red)
        }

    }
}

@Composable
actual fun OpenedCell(cell: Cell) {
    Text(
        text = cell.bombsNear.toString(),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
actual fun NewGameButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .background(color = Color(0x42, 0x8e, 0x04))
            .border(width = 1.dp,  color = Color.White)
            .clickable { onClick() }
    ) {
        androidx.compose.material.Text(text,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
actual fun ClickableCell(
    onLeftMouseButtonClick: (isShiftPressed: Boolean) -> Unit,
    onRightMouseButtonClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .pointerInput(PointerEventType.Press) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        with(event) {
                            println("Event $type")
                            if (type == PointerEventType.Press) {
                                println("PRessed")
                                val lmb = buttons.isPrimaryPressed
                                val rmb = buttons.isSecondaryPressed

                                if (lmb && !rmb) {
                                    onLeftMouseButtonClick(keyboardModifiers.isShiftPressed)
                                } else if (rmb && !lmb) {
                                    onRightMouseButtonClick()
                                }
                            }
                        }
                    }
                }
            }
//            .mouseClickable {
//                val lmb = buttons.isPrimaryPressed
//                val rmb = buttons.isSecondaryPressed
//
//                if (lmb && !rmb) {
//                    onLeftMouseButtonClick(keyboardModifiers.isShiftPressed)
//                } else if (rmb && !lmb) {
//                    onRightMouseButtonClick()
//                }
//            }
    ) {
        content()
    }
}