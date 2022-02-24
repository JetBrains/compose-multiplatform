package org.jetbrains.compose.kapp.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.kapp.simpleKapp

fun simpleDemoApp() = simpleKapp(name = "Kapp demo") {
    SimpleAppContent()
}

@Composable
fun SimpleAppContent() {
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