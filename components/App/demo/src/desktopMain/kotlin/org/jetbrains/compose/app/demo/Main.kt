package org.jetbrains.compose.app.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*

import org.jetbrains.compose.app.*

fun main() = app(
    title = "App demo"
) {
    var visible by remember { mutableStateOf(5) }

    for (index in 0 .. visible) {
        Frame {
            Column {
                Text("I am frame $index")
                if (index == 0) {
                    Button(onClick = { visible++ }) {
                        Text("Add")
                    }
                    Button(onClick = { if (visible > 1) visible-- }) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}
