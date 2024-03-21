package org.jetbrains.compose.splitpane.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
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
        var delta by remember { mutableStateOf("20") }
        var percentage by remember { mutableStateOf("0.20") }
        Row {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(10.dp).width(180.dp)) {
                Text("Action panel", fontWeight = FontWeight.Bold)
                Button(onClick = { splitterState.moveEnabled = !splitterState.moveEnabled }) {
                    Text(if (splitterState.moveEnabled) "Freeze V" else "Unfreeze V")
                }
                Button(onClick = { hSplitterState.moveEnabled = !hSplitterState.moveEnabled }) {
                    Text(if (hSplitterState.moveEnabled) "Freeze H" else "Unfreeze H")
                }

                OutlinedTextField(value = delta, onValueChange = { delta = it }, label = { Text("Delta") })
                Button(onClick = { delta.toFloatOrNull()?.let { splitterState.dispatchRawMovement(it) } }) {
                    Text("Add delta V")
                }
                Button(onClick = { delta.toFloatOrNull()?.let { hSplitterState.dispatchRawMovement(it) } }) {
                    Text("Add delta H")
                }

                OutlinedTextField(value = percentage, onValueChange = { percentage = it }, label = { Text("Fraction") })
                Button(onClick = { percentage.toFloatOrNull()?.let { splitterState.positionPercentage = it } }) {
                    Text("Set fraction V")
                }
                Button(onClick = { percentage.toFloatOrNull()?.let { hSplitterState.positionPercentage = it } }) {
                    Text("Set fraction H")
                }
            }
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
}