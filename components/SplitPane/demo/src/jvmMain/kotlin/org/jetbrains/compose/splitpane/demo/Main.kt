package org.jetbrains.compose.splitpane.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.splitpane.*
import java.awt.Cursor

private fun Modifier.cursorForHorizontalResize(): Modifier =
    pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

@OptIn(ExperimentalSplitPaneApi::class)
fun main() = singleWindowApplication(
    title = "SplitPane demo"
) {
    MaterialTheme {
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
                    Box(
                        Modifier
                            .markAsHandle()
                            .cursorForHorizontalResize()
                            .background(SolidColor(Color.Gray), alpha = 0.50f)
                            .width(9.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}