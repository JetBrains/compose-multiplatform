package com.github.veselovalex.minesweeper

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.ui.Styles

fun main() {
    val root = document.getElementById("app-root") ?: throw RuntimeException("#app-root is missing in index.html")

    renderComposable(root) {
        Style(Styles)
        Game()
    }
}


@Composable
actual fun CellWithIcon(src: String, alt: String) {
    Img(src, alt, attrs = {
        style {
            property("user-select", "none")
            margin(2.px)
            width(28.px)
            height(28.px)
        }
    })
}

@Composable
actual fun OpenedCell(cell: Cell) {
    Div (
        attrs = {
            style {
                property("user-select", "none")
                fontSize(28.px)
                lineHeight("1")
                fontWeight("bold")
                fontFamily("sans-serif")
                textAlign("center")
                width(28.px)
                height(28.px)
                boxSizing("border-box")
                margin(2.px)
            }
        }
    ) {
        Text(cell.bombsNear.toString())
    }
}

@Composable
actual fun ClickableCell(
    onLeftMouseButtonClick: (isShiftPressed: Boolean) -> Unit,
    onRightMouseButtonClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Div (
        attrs = {
            onClick {
                it.preventDefault()
                onLeftMouseButtonClick(it.shiftKey)
            }
            onContextMenu {
                // Handle right mouse button click
                // Disable default context menu
                it.preventDefault()
                onRightMouseButtonClick()
            }
            style {
                cursor("pointer")
                boxSizing("border-box")
            }
        }
    ) {
        content()
    }
}

@Composable
actual fun NewGameButton(text: String, onClick: () -> Unit) {
    Button (
        attrs = {
            style {
                background("green")
                color(Color.white)
                font("18px/1 sans-serif")
                cursor("pointer")
                padding(4.px, 8.px)
                border(1.px, LineStyle.Solid, Color.white)
            }

            onClick { it.preventDefault(); onClick() }
        }
    ) {
        Text(text)
    }
}