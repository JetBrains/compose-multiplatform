package org.jetbrains.compose.intellij.platform.sample

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.TextButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Buttons() {
    Row {
        val btnEnabled = remember { mutableStateOf(true) }
        Button(
            onClick = { btnEnabled.value = !btnEnabled.value},
            modifier = Modifier.padding(8.dp),
            enabled = btnEnabled.value
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "FavoriteBorder",
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(text = "Button")
        }
        val btnTextEnabled = remember { mutableStateOf(true) }
        TextButton(
            onClick = { btnTextEnabled.value = !btnTextEnabled.value },
            modifier = Modifier.padding(8.dp),
            enabled = btnTextEnabled.value
        ) {
            Text(text = "Text Button")
        }
        OutlinedButton(
            onClick = {
                btnEnabled.value = true
                btnTextEnabled.value = true
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.padding(0.dp)
            )
        }
    }
}