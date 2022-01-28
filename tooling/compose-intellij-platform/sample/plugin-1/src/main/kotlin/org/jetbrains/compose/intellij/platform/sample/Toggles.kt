package org.jetbrains.compose.intellij.platform.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Toggles() {
    Column {
        Row {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
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
            }
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                var selected by remember { mutableStateOf("Kotlin") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == "Kotlin", onClick = { selected = "Kotlin" })
                    Text(
                        text = "Kotlin",
                        modifier = Modifier.clickable(onClick = { selected = "Kotlin" }).padding(start = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == "Java", onClick = { selected = "Java" })
                    Text(
                        text = "Java",
                        modifier = Modifier.clickable(onClick = { selected = "Java" }).padding(start = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == "Swift", onClick = { selected = "Swift" })
                    Text(
                        text = "Swift",
                        modifier = Modifier.clickable(onClick = { selected = "Swift" }).padding(start = 4.dp)
                    )
                }
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
            onValueChange = { newValue ->
                sliderState2 = newValue
            }
        )
    }
}