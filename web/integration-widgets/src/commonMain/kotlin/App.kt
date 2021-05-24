package org.jetbrainsc.compose.common.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.foundation.layout.Box
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.size
import org.jetbrains.compose.common.ui.background
import org.jetbrains.compose.common.ui.padding
import org.jetbrains.compose.common.ui.unit.dp
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.foundation.layout.Row
import org.jetbrains.compose.common.foundation.layout.Column
import org.jetbrains.compose.common.foundation.layout.Arrangement
import org.jetbrains.compose.common.ui.Alignment
import org.jetbrains.compose.common.foundation.border
import org.jetbrains.compose.common.material.Text
import org.jetbrains.compose.common.ui.unit.em
import org.jetbrains.compose.common.material.Slider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.common.material.Button
import org.jetbrains.compose.common.foundation.layout.width
import org.jetbrains.compose.common.foundation.clickable
import org.jetbrains.compose.common.ui.draw.clip
import jetbrains.compose.common.shapes.CircleShape

object LayoutSamples {
    @Composable
    fun TwoTexts() {
        Text("Alfred Sisley")
        Text("3 minutes ago")
    }

    @Composable
    fun TwoTextsInColumn() {
        val defaultFontSize = 0.79f
        val fontSize = remember { mutableStateOf(defaultFontSize) }
        val subtitleColor = remember { mutableStateOf(Color(0, 0, 200)) }
        Column {
            Text("Alfred Sisley")
            Text(
                "3 minutes ago",
                color = subtitleColor.value,
                size = fontSize.value.em,
                modifier = Modifier.clickable {
                    subtitleColor.value = Color.Yellow
                }
            )
            Slider(
                fontSize.value,
                onValueChange = { value ->
                    fontSize.value = value
                },
                valueRange = 0.1f..1.2f,
                steps = 80,
                modifier = Modifier.width(200.dp)
            )
            Button(
                onClick = {
                    fontSize.value = defaultFontSize
                }
            ) {
                Text("reset view")
            }
        }
    }

    @Composable
    fun TwoTextsInRow() {
        Text("Alfred Sisley")
        Text("3 minutes ago")
    }

    @Composable
    fun Layouts() {
        val horizontalArrangements = listOf(Arrangement.Start, Arrangement.End)
        val verticalAlignments = listOf(Alignment.Top, Alignment.CenterVertically, Alignment.Bottom)
        Column() {
            horizontalArrangements.forEach { horizontalArrangement ->
                verticalAlignments.forEach { verticalAlignment ->
                    Row(
                        modifier = Modifier
                            .size(150.dp)
                            .padding(4.dp)
                            .border(1.dp, Color(0, 0, 200))
                            .background(Color.Yellow),
                        horizontalArrangement = horizontalArrangement,
                        verticalAlignment = verticalAlignment
                    ) {
                        Box(Modifier.size(50.dp).background(Color.Red)) { }
                        Box(Modifier.size(30.dp).background(Color.Blue)) { }
                    }
                }
            }
        }
    }

    @Composable
    fun LayoutsClipped() {
        val horizontalArrangements = listOf(Arrangement.Start, Arrangement.End)
        val verticalAlignments = listOf(Alignment.Top, Alignment.CenterVertically, Alignment.Bottom)
        Column() {
            horizontalArrangements.forEach { horizontalArrangement ->
                verticalAlignments.forEach { verticalAlignment ->
                    Row(
                        modifier = Modifier
                            .size(150.dp)
                            .padding(4.dp)
                            .border(1.dp, Color(0, 0, 200))
                            .background(Color.Yellow),
                        horizontalArrangement = horizontalArrangement,
                        verticalAlignment = verticalAlignment
                    ) {
                        Box(Modifier.size(50.dp).background(Color.Red)) { }
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .size(30.dp)
                                .background(Color.Blue)
                        ) { }
                    }
                }
            }
        }
    }
}

@Composable
fun App() {
    LayoutSamples.TwoTextsInColumn()
}