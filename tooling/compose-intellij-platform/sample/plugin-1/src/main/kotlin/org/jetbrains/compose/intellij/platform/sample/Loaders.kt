package org.jetbrains.compose.intellij.platform.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Loaders() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Box(
            modifier = Modifier.height(30.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp, 20.dp),
                strokeWidth = 4.dp
            )
        }
        Box(
            modifier = Modifier
                .height(30.dp)
                .padding(start = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}