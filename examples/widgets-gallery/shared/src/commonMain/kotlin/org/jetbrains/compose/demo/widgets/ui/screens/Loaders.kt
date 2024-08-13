package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.demo.widgets.ui.WidgetsType

@Composable
fun Loaders() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .testTag(WidgetsType.LOADERS.testTag),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()

        CircularProgressIndicator(strokeWidth = 8.dp)

        LinearProgressIndicator()

        Column {
            LinearProgressIndicator()
            Text(text = "Loading with text...", modifier = Modifier.padding(8.dp))
        }
    }
}
