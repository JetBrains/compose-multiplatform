package org.jetbrains.compose.splitpane.demo

import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.LocalAppWindow
import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.awt.Cursor

private fun Modifier.cursorForHorizontalResize(
): Modifier = composed {
    var isHover by remember { mutableStateOf(false) }

    if (isHover) {
        LocalAppWindow.current.window.cursor = Cursor(Cursor.E_RESIZE_CURSOR)
    } else {
        LocalAppWindow.current.window.cursor = Cursor.getDefaultCursor()
    }

    pointerMoveFilter(
        onEnter = { isHover = true; true },
        onExit = { isHover = false; true }
    )
}

fun main() = Window(
    "SplitPane demo"
) {
    MaterialTheme {
        DesktopTheme {
            val splitterState = rememberSplitPaneState()
            val hSplitterState = rememberSplitPaneState()
            HorizontalSplitPane(
                splitPaneState = splitterState
            ) {
                first(20.dp) {
                    Box(Modifier.background(Color.Red).fillMaxSize())
                }
                second(50.dp) {
                    VerticalSplitPane(splitPaneState = hSplitterState) {
                        first(50.dp) {
                            Box(Modifier.background(Color.Blue).fillMaxSize())
                        }
                        second(20.dp) {
                            Box(Modifier.background(Color.Green).fillMaxSize())
                        }
                    }
                }
                splitter {
                    visiblePart {
                        Box(
                            Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colors.background)
                        )
                    }
                    handle {
                        {
                            Box(
                                Modifier
                                    .markAsHandle()
                                    .cursorForHorizontalResize()
                                    .background(SolidColor(Color.Gray), alpha = 0.50f)
                                    .width(8.dp)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}