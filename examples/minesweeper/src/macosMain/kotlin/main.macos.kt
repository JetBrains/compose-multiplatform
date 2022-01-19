import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import platform.AppKit.NSApplication
import platform.AppKit.NSApp
import platform.Foundation.NSBundle

fun main() {
    NSApplication.sharedApplication()
    Window("Minesweeper") {
        MaterialTheme {
            Game(
                requestWindowSize = { w, h -> /* TODO(make resizable) */}
            )
        }
    }
    NSApp?.run()
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
    // TODO Bundle pics and show images properly
    val color = when (src) {
        "assets/clock.png" -> Color.Blue
        "assets/flag.png" -> Color.Green
        "assets/mine.png" -> Color.Red
        else -> Color.White
    }
    return object : Painter() {
        override val intrinsicSize: Size
            get() = Size(16f, 16f)

        override fun DrawScope.onDraw() {
            drawRect(color = color)
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
        Text(text,
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
            .clickable { onLeftMouseButtonClick(false) } // TODO unneccessary if pointerInput works
            .pointerInput(onLeftMouseButtonClick, onRightMouseButtonClick) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        with(event) {
                            if (type == PointerEventType.Press) {
                                // TODO does not work yet, all events are of Unknown type (
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
    ) {
        content()
    }
}