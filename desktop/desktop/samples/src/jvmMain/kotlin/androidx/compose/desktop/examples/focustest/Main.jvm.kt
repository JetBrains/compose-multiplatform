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

package androidx.compose.desktop.examples.focustest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.SwingUtilities

fun main() = SwingUtilities.invokeLater {
    val window = JFrame()
    window.preferredSize = Dimension(300, 800)
    window.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
    window.contentPane.add(javax.swing.Box.createVerticalBox().apply {
        add(JButton())
        add(JButton())

        add(ComposePanel().apply {
            setContent {
                MaterialTheme {
                    Column(Modifier.fillMaxSize()) {
                        Button({}) {}
                        Button({}) {}
                        TextField("", {}, singleLine = true)
                        SwingPanel(
                            modifier = Modifier.size(100.dp),
                            factory = {
                                javax.swing.Box.createVerticalBox().apply {
                                    add(JButton())
                                    add(JButton())
                                    add(JButton())
                                }
                            }
                        )
                        Button({}) {}

                        SwingPanel(
                            modifier = Modifier.size(100.dp),
                            factory = {
                                javax.swing.Box.createVerticalBox().apply {
                                    add(JButton())
                                    ComposePanel().apply {
                                        setContent {
                                            MaterialTheme {
                                                Column(Modifier.fillMaxSize()) {
                                                    Button({}) {}
                                                    SwingPanel(
                                                        modifier = Modifier.size(100.dp),
                                                        factory = {
                                                            javax.swing.Box.createVerticalBox().apply {
                                                                add(JButton())
                                                                add(JButton())
                                                            }
                                                        }
                                                    )
                                                    Button({}) {}
                                                }
                                            }
                                        }
                                    }
                                    add(JButton())
                                }
                            }
                        )

                        Button({}) {}
                    }
                }
            }
        })
        add(JButton())
        add(JButton())
    })

    window.pack()
    window.isVisible = true
}
