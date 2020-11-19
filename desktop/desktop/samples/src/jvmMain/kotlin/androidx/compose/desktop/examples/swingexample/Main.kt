/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.desktop.examples.swingexample

import androidx.compose.desktop.AppManager
import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.AppWindowAmbient
import androidx.compose.desktop.ComposePanel
import androidx.compose.desktop.setContent
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.ScrollableRow
import androidx.compose.foundation.Text
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material.Button
import androidx.compose.material.ButtonConstants
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JFrame
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

val nCount = mutableStateOf(0)
val nButton = Button("North")

val eCount = mutableStateOf(0)
val eButton = Button("East")

val wCount = mutableStateOf(0)
val wButton = Button("West")

val sCount = mutableStateOf(0)
val sButton = Button("South")

val layer = ComposePanel()
val window = JFrame()

fun main() = SwingUtilities.invokeLater {
    // explicitly clear the application events
    AppManager.setEvents(
        onAppStart = null,
        onAppExit = null,
        onWindowsEmpty = null
    )
    SwingComposeWindow("ComposeIntoSwing")
}

fun SwingComposeWindow(title: String) {
    window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    window.title = title

    window.contentPane.add(actionButton("NORTH", { nCount.value++ }), BorderLayout.NORTH)
    window.contentPane.add(actionButton("WEST", { wCount.value++ }), BorderLayout.WEST)
    window.contentPane.add(actionButton("EAST", { eCount.value++ }), BorderLayout.EAST)
    window.contentPane.add(
        actionButton(
            text = "SOUTH/REMOVE COMPOSE",
            action = {
                sCount.value++
                window.contentPane.remove(layer)
            }
        ),
        BorderLayout.SOUTH
    )

    window.contentPane.add(layer, BorderLayout.CENTER)

    layer.setContent {
        ComposeContent()
    }

    window.setSize(800, 600)
    window.setVisible(true)
}

fun actionButton(text: String, action: (() -> Unit)? = null): JButton {
    val button = Button(text)
    button.addActionListener(object : ActionListener {
        public override fun actionPerformed(e: ActionEvent) {
            action?.invoke()
        }
    })

    return button
}

fun Button(text: String): JButton {
    val btn = JButton(text)
    btn.setToolTipText("Tooltip for button $text")
    btn.setPreferredSize(Dimension(100, 100))
    return btn
}

fun Panel(): JPanel {
    val btn = JPanel()
    btn.setBackground(java.awt.Color.RED)
    btn.setPreferredSize(Dimension(100, 100))
    return btn
}

@Composable
fun ComposeContent() {
    Column {
        TextBox(
            text = "NorthClicks:${nCount.value} " +
                "SouthClicks:${sCount.value} " +
                "WestClicks:${wCount.value} " +
                "EastClicks:${eCount.value}"
        )
        Spacer(modifier = Modifier.height(25.dp))
        Row {
            CircularProgressIndicator(Modifier.preferredSize(25.dp, 25.dp).padding(end = 3.dp))
            Button(
                text = "New window...",
                onClick = {
                    AppWindow(
                        title = "Second window",
                        size = IntSize(400, 200),
                        onDismissRequest = {
                            println("Second window is dismissed.")
                        }
                    ).show {
                        WindowContent(
                            amount = nCount
                        )
                    }
                },
                color = Color(26, 198, 188)
            )
        }
        Spacer(modifier = Modifier.height(25.dp))
        Box(
            modifier = Modifier.fillMaxSize()
                .background(color = Color(180, 180, 180))
                .padding(10.dp)
        ) {
            val stateVertical = rememberScrollState(0f)
            val stateHorizontal = rememberScrollState(0f)
            ScrollableColumn(
                modifier = Modifier.fillMaxSize()
                    .padding(end = 12.dp, bottom = 12.dp),
                scrollState = stateVertical
            ) {
                ScrollableRow(scrollState = stateHorizontal) {
                    Column {
                        for (item in 0..30) {
                            ListItem("Item in ScrollableColumn #$item")
                            if (item < 30) {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(stateVertical)
            )
            HorizontalScrollbar(
                modifier = Modifier.align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                adapter = rememberScrollbarAdapter(stateHorizontal)
            )
        }
    }
}

@Composable
fun ListItem(text: String = "Item", color: Color = Color(0, 0, 0, 20), width: Int = 800) {
    Box(
        modifier = Modifier.height(32.dp)
            .width(width.dp)
            .background(color = color)
            .padding(start = 10.dp),
        alignment = Alignment.CenterStart
    ) {
        Text(text = text)
    }
}

@Composable
fun TextBox(text: String = "") {
    Box(
        modifier = Modifier.height(30.dp),
        alignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(200, 200, 200)
        )
    }
}

@Composable
fun Button(
    text: String = "",
    onClick: () -> Unit = {},
    color: Color = Color(10, 162, 232),
    size: IntSize = IntSize(150, 30)
) {
    val buttonHover = remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        colors = ButtonConstants.defaultButtonColors(
            backgroundColor =
                if (buttonHover.value)
                    Color(color.red / 1.3f, color.green / 1.3f, color.blue / 1.3f)
                else
                    color
        ),
        modifier = Modifier
            .preferredSize(size.width.dp, size.height.dp)
            .hover(
                onEnter = {
                    buttonHover.value = true
                    false
                },
                onExit = {
                    buttonHover.value = false
                    false
                },
                onMove = { false }
            )
    ) {
        Text(text = text)
    }
}

@Composable
fun WindowContent(amount: MutableState<Int>) {
    val window = AppWindowAmbient.current
    Box(
        Modifier.fillMaxSize().background(color = Color(55, 55, 55)),
        alignment = Alignment.Center
    ) {
        Column {
            TextBox(text = "Increment NorthClicks?")
            Spacer(modifier = Modifier.height(30.dp))
            Row {
                Button(text = "Yes", onClick = { amount.value++ })
                Spacer(modifier = Modifier.width(30.dp))
                Button(text = "Close", onClick = { window?.close() })
            }
        }
    }
}

fun Modifier.hover(
    onEnter: () -> Boolean,
    onExit: () -> Boolean,
    onMove: (Offset) -> Boolean
): Modifier = this.pointerMoveFilter(onEnter = onEnter, onExit = onExit, onMove = onMove)