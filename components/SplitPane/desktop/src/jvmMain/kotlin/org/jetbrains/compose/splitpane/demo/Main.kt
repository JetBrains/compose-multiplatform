package org.jetbrains.compose.splitpane.demo

import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.cursorForResize
import org.jetbrains.compose.splitpane.rememberSplitPaneState

@Composable
private fun WithoutTouchSlop(content: @Composable () -> Unit) {
    fun ViewConfiguration.withoutTouchSlop() = object : ViewConfiguration {
        override val longPressTimeoutMillis
            get() =
                this@withoutTouchSlop.longPressTimeoutMillis

        override val doubleTapTimeoutMillis
            get() =
                this@withoutTouchSlop.doubleTapTimeoutMillis

        override val doubleTapMinTimeMillis
            get() =
                this@withoutTouchSlop.doubleTapMinTimeMillis

        override val touchSlop: Float get() = 0f
    }

    CompositionLocalProvider(
        LocalViewConfiguration provides LocalViewConfiguration.current.withoutTouchSlop()
    ) {
        content()
    }
}

fun main() = Window(
    "SplitPane demo"
) {
    MaterialTheme {
        DesktopTheme {
            WithoutTouchSlop {
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
                                        .cursorForResize(true)
                                        .background(SolidColor(Color.Gray),alpha = 0.50f)
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
}