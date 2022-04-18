package androidx.compose.mpp.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler

@Composable
fun myContent() {
    var tick by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(false) }
    var clutz by remember { mutableStateOf(false) }
    var switched by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("click for clipboard") }
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    Column {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(color = if (selected) Color.Gray else Color.Red)
                .width(100.dp).height(100.dp)
                .clickable {
                    println("Red box: clicked")
                }
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .background(color = if (tick) Color.Green else Color.Blue)
                    .width(20.dp).height(20.dp)
                    .clickable {
                        println("Small box: clicked")
                    }
            )
        }
        Spacer(
            Modifier.width(200.dp)
                .height(if (clutz) 4.dp else 12.dp)
                .background(color = if (clutz) Color.DarkGray else Color.Magenta)
        )
        Button(
            modifier = Modifier
                .padding(16.dp),
            onClick = {
                println("Button clicked!")
                tick = !tick
            }
        ) {
            Text(if (switched) "🦑 press 🐙" else "Press me!")
        }
        Row {
            RadioButton(
                modifier = Modifier
                    .padding(16.dp),
                selected = selected,
                onClick = {
                    println("RadioButton clicked!")
                    selected = !selected
                }
            )

            Checkbox(
                checked = clutz,
                modifier = Modifier.padding(16.dp),
                onCheckedChange = { clutz = !clutz }
            )
        }
        Switch(
            modifier = Modifier
                .padding(16.dp),
            checked = switched,
            onCheckedChange = { switched = it }
        )
        Row {
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    uriHandler.openUri("https://kotlinlang.org")
                },
            ) {
                Text("Open URL")
            }
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    text = clipboard.getText()?.text ?: "clipboard is empty"
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
            ) {
                Text(text)
            }
        }

    }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                // println("NANO: $it, tick: $tick")
            }
        }
    }
}
