package org.jetbrains.compose.common.material

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button

@Composable
actual fun ButtonActual(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Button(
        attrs = {
            onClick { onClick() }
        }
    ) {
        content()
    }
}
