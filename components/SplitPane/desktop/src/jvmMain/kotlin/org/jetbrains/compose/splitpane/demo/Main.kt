package org.jetbrains.compose.splitpane.demo

import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitterState

fun main() = Window(
    "SplitPane demo"
) {
    MaterialTheme {
        DesktopTheme {
            val splitterState = rememberSplitterState(50.dp)
            val hSplitterState = rememberSplitterState(50.dp)
            HorizontalSplitPane(
                splitterState
            ) {
                first(20.dp) {
                    Box(Modifier.background(Color.Red).fillMaxSize())
                }
                second(50.dp) {
                    VerticalSplitPane(hSplitterState) {
                        first(50.dp) {
                            Box(Modifier.background(Color.Blue).fillMaxSize())
                        }
                        second(20.dp) {
                            Box(Modifier.background(Color.Green).fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}