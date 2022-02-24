package org.jetbrains.compose.kapp.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*

import org.jetbrains.compose.kapp.*

fun multiFrameApp() = kapp(title = "Kapp demo") {
    var visible = remember { mutableStateOf(5) }

    for (index in 0 .. visible.value) {
        Frame {
            FrameContent(index, visible)
        }
    }
}

@Composable
fun FrameContent(index: Int, visible: MutableState<Int>) {
    Column {
        Text("I am frame $index")
        if (index == 0) {
            Button(onClick = { visible.value++ }) {
                Text("Add")
            }
            Button(onClick = { if (visible.value > 1) visible.value-- }) {
                Text("Remove")
            }
        }
    }
}
