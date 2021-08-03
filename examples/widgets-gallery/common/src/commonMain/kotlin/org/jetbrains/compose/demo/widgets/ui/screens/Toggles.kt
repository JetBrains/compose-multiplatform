package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Toggles() {
    Column {
        var checked by remember { mutableStateOf(true) }
        Checkbox(
            checked = checked,
            modifier = Modifier.padding(8.dp),
            onCheckedChange = { checked = !checked }
        )

        var switched by remember { mutableStateOf(true) }
        Switch(
            checked = switched,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
            modifier = Modifier.padding(8.dp),
            onCheckedChange = { switched = it }
        )

        AlignedColumn {
            var selected by remember { mutableStateOf("Kotlin") }

            Row {
                RadioButton(selected = selected == "Kotlin", onClick = { selected = "Kotlin" })
                Text(
                    text = "Kotlin",
                    modifier = Modifier.clickable(onClick = { selected = "Kotlin" }).padding(start = 4.dp)
                )
            }
            //Spacer(modifier = Modifier.size(4.dp))
            Row {
                RadioButton(selected = selected == "Java", onClick = { selected = "Java" })
                Text(
                    text = "Java",
                    modifier = Modifier.clickable(onClick = { selected = "Java" }).padding(start = 4.dp)
                )
            }
            //Spacer(modifier = Modifier.size(4.dp))
            Row {
                RadioButton(selected = selected == "Swift", onClick = { selected = "Swift" })
                Text(
                    text = "Swift",
                    modifier = Modifier.clickable(onClick = { selected = "Swift" }).padding(start = 4.dp)
                )
            }
        }

        var sliderState by remember { mutableStateOf(0f) }
        Slider(value = sliderState, modifier = Modifier.fillMaxWidth().padding(8.dp),
            onValueChange = { newValue ->
                sliderState = newValue
            }
        )

        var sliderState2 by remember { mutableStateOf(20f) }
        Slider(value = sliderState2, modifier = Modifier.fillMaxWidth().padding(8.dp),
            valueRange = 0f..100f,
            steps = 5,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colors.secondary),
            onValueChange = { newValue ->
                sliderState2 = newValue
            }
        )
    }
}

@Composable
private fun AlignedColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        content()
    }
}