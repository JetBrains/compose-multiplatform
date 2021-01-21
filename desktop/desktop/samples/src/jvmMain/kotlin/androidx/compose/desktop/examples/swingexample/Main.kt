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
import androidx.compose.desktop.AppWindowAmbient
import androidx.compose.desktop.ComposePanel
import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.WindowConstants

val northClicks = mutableStateOf(0)
val westClicks = mutableStateOf(0)
val eastClicks = mutableStateOf(0)

fun main() {
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
    val composePanel = ComposePanel()

    // setting the content
    composePanel.setContent {
        ComposeContent()
        DisposableEffect(Unit) {
            onDispose {
                println("Dispose composition")
            }
        }
    }

    val window = JFrame()
    window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    window.title = "SwingComposeWindow"

    window.contentPane.add(actionButton("NORTH", { northClicks.value++ }), BorderLayout.NORTH)
    window.contentPane.add(actionButton("WEST", { westClicks.value++ }), BorderLayout.WEST)
    window.contentPane.add(actionButton("EAST", { eastClicks.value++ }), BorderLayout.EAST)
    window.contentPane.add(
        actionButton(
            text = "SOUTH/REMOVE COMPOSE",
            action = {
                window.contentPane.remove(composePanel)
            }
        ),
        BorderLayout.SOUTH
    )

    // addind ComposePanel on JFrame
    window.contentPane.add(composePanel, BorderLayout.CENTER)

    window.setSize(800, 600)
    window.setVisible(true)
}

fun actionButton(text: String, action: (() -> Unit)? = null): JButton {
    val button = JButton(text)
    button.setToolTipText("Tooltip for $text button.")
    button.setPreferredSize(Dimension(100, 100))
    button.addActionListener(object : ActionListener {
        public override fun actionPerformed(e: ActionEvent) {
            action?.invoke()
        }
    })

    return button
}

@Composable
fun ComposeContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Row {
                Button(
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
    val window = AppWindowAmbient.current
    Box(
        Modifier.fillMaxSize().background(color = Color(55, 55, 55)),
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
                Button(onClick = { window?.close() }) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}
