package org.jetbrains.compose.app.demo

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.app.composeApp

fun main() = composeApp(name = "App demo") {
    Content()
}

@Preview
@Composable
fun Content() {
    var counter by remember { mutableStateOf(0) }
    Column {
        Text("I an app: $counter")
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { counter++ }) {
                Text("More")
            }
            Spacer(Modifier.width(20.dp))
            Button(
                onClick = { counter-- }) {
                Text("Less")
            }

        }
    }
}