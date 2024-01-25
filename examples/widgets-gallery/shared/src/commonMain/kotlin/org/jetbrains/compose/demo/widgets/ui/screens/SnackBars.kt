package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.demo.widgets.ui.WidgetsType

@Composable
fun SnackBars() {
    Column(
        modifier = Modifier
            .padding(4.dp)
            .testTag(WidgetsType.SNACK_BARS.testTag),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Snackbar {
            Text(text = "This is a basic snackbar")
        }
        Snackbar(
            action = {
                TextButton(onClick = {}) {
                    Text(text = "Remove")
                }
            }
        ) {
            Text(text = "This is a basic snackbar with action item")
        }
        Snackbar(
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
}
