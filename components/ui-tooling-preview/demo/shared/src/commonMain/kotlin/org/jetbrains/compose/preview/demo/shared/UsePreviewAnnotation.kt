package org.jetbrains.compose.preview.demo.shared

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun UsedInPreview() {
    Text("This is commonMain Composable function")
}

@Preview
@Composable
fun UsePreviewAnnotation() {
    UsedInPreview()
}
