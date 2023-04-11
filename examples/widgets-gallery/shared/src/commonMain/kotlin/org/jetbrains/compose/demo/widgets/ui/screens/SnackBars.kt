package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.demo.widgets.theme.typography

@Composable
fun SnackBars() {
    Snackbar(modifier = Modifier.padding(4.dp)) {
        Text(text = "This is a basic snackbar")
    }
    Snackbar(
        modifier = Modifier.padding(4.dp),
        action = {
            TextButton(onClick = {}) {
                Text(text = "Remove")
            }
        }
    ) {
        Text(text = "This is a basic Snackbar with action item")
    }
    Snackbar(
        modifier = Modifier.padding(4.dp),
        actionOnNewLine = true,
        action = {
            TextButton(onClick = {}) {
                Text(text = "Remove")
            }
        }
    ) {
        Text(text = "Snackbar with action item below text")
    }
}
