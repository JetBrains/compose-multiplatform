package com.github.veselovalex.minesweeper

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        resizable = false,
        title = "Minesweeper",
        icon = painterResource("assets/mine.png"),
        state = windowState
    ) {
        DesktopMaterialTheme {
            Game(
                requestWindowSize = { w, h ->
                    windowState.size = windowState.size.copy(width = w.dp, height = h.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun ClickableCell(
    onLeftMouseButtonClick: (isShiftPressed: Boolean) -> Unit,
    onRightMouseButtonClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .mouseClickable {
                val lmb = buttons.isPrimaryPressed
                val rmb = buttons.isSecondaryPressed

                if (lmb && !rmb) {
                    onLeftMouseButtonClick(keyboardModifiers.isShiftPressed)
                } else if (rmb && !lmb) {
                    onRightMouseButtonClick()
                }
            }
    ) {
        content()
    }
}