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
import androidx.compose.desktop.LocalAppWindow
import androidx.compose.desktop.ComposePanel
import androidx.compose.desktop.SwingPanel
import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.awt.BorderLayout
import java.awt.Color as awtColor
import java.awt.Component
import java.awt.GridLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

val northClicks = mutableStateOf(0)
val westClicks = mutableStateOf(0)
val eastClicks = mutableStateOf(0)

fun main() = SwingUtilities.invokeLater {
    // explicitly clear the application events
    AppManager.setEvents(
        onAppStart = null,
        onAppExit = null,
        onWindowsEmpty = null
    )
    SwingComposeWindow()
}

fun SwingComposeWindow() {
    // creating ComposePanel
    val composePanelTop = ComposePanel()
    composePanelTop.setBackground(awtColor(55, 155, 55))

    val composePanelBottom = ComposePanel()
    composePanelBottom.setBackground(awtColor(55, 55, 155))

    // setting the content
    composePanelTop.setContent {
        ComposeContent(background = Color(55, 155, 55))
        DisposableEffect(Unit) {
            onDispose {
                println("Dispose composition")
            }
        }
    }
    composePanelBottom.setContent {
        ComposeContent(background = Color(55, 55, 155))
        DisposableEffect(Unit) {
            onDispose {
                println("Dispose composition")
            }
        }
    }

    val window = JFrame()
    window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    window.title = "SwingComposeWindow"

    val panel = JPanel()
    panel.setLayout(GridLayout(2, 1))
    window.contentPane.add(panel, BorderLayout.CENTER)

    window.contentPane.add(actionButton("WEST", { westClicks.value++ }), BorderLayout.WEST)
    window.contentPane.add(
        actionButton(
            text = "SOUTH/REMOVE COMPOSE",
            size = IntSize(40, 40),
            action = {
                panel.remove(composePanelBottom)
            }
        ),
        BorderLayout.SOUTH
    )

    // addind ComposePanel on JFrame
    panel.add(composePanelTop)
    panel.add(composePanelBottom)

    window.setSize(800, 600)
    window.setVisible(true)
}

fun actionButton(
    text: String,
    action: (() -> Unit)? = null,
    size: IntSize = IntSize(70, 70)
): JButton {
    val button = JButton(text)
    button.setToolTipText("Tooltip for $text button.")
    button.setPreferredSize(Dimension(size.width, size.height))
    button.addActionListener(object : ActionListener {
        public override fun actionPerformed(e: ActionEvent) {
            action?.invoke()
        }
    })

    return button
}

@Composable
fun ComposeContent(background: Color = Color.White) {
    Box(
        modifier = Modifier.fillMaxSize().background(color = background),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Row(
                modifier = Modifier.height(40.dp)
            ) {
                Button(
                    modifier = Modifier.height(35.dp).padding(top = 3.dp),
                    onClick = {
                        Window(
                            size = IntSize(400, 250)
                        ) {
                            SecondWindowContent()
                        }
                    }
                ) {
                    Text("New window...", color = Color.White)
                }
                Spacer(modifier = Modifier.width(20.dp))
                SwingPanel(
                    modifier = Modifier.preferredSize(200.dp, 39.dp),
                    componentBlock = {
                        actionButton(
                            text = "JComponent",
                            action = {
                                westClicks.value++
                                northClicks.value++
                                eastClicks.value++
                            }
                        )
                    },
                    background = background
                )
                Spacer(modifier = Modifier.width(20.dp))
                SwingPanel(
                    background = background,
                    modifier = Modifier.preferredSize(200.dp, 39.dp),
                    componentBlock = { ComposableColoredPanel(Color.Red) }
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
            Row {
                Counter("West", westClicks)
                Spacer(modifier = Modifier.width(25.dp))
                Counter("North", northClicks)
                Spacer(modifier = Modifier.width(25.dp))
                Counter("East", eastClicks)
            }
        }
    }
}

fun ComposableColoredPanel(color: Color): Component {
    val composePanel = ComposePanel()

    // setting the content
    composePanel.setContent {
        Box(
            modifier = Modifier.fillMaxSize().background(color = color),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "ColoredPanel")
        }
    }

    return composePanel
}

@Composable
fun Counter(text: String, counter: MutableState<Int>) {
    Surface(
        modifier = Modifier.size(130.dp, 130.dp),
        color = Color(180, 180, 180),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier.height(30.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "${text}Clicks: ${counter.value}")
            }
            Spacer(modifier = Modifier.height(25.dp))
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { counter.value++ }) {
                    Text(text = text, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SecondWindowContent() {
    val window = LocalAppWindow.current
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Box(
                modifier = Modifier.height(30.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Second Window", color = Color.White)
            }
            Spacer(modifier = Modifier.height(30.dp))
            Box(
                modifier = Modifier.height(30.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { window.close() }) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}