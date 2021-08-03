package org.jetbrains.compose.demo.widgets.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Loaders() {
    AlignedColumn {
        CircularProgressIndicator()
    }

    AlignedColumn {
        CircularProgressIndicator(strokeWidth = 8.dp)
    }

    AlignedColumn {
        LinearProgressIndicator()
    }

    AlignedColumn {
        LinearProgressIndicator()
        Text(text = "Loading with text...", modifier = Modifier.padding(8.dp))
    }
}
@Composable
private fun AlignedColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        content()
    }
}