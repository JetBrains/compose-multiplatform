package org.jetbrains.compose.common.material

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.web.elements.Button
import org.jetbrains.compose.common.ui.asStyleBuilderApplier

@Composable
actual fun ButtonActual(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Button(
        style = modifier.asStyleBuilderApplier(),
        attrs = {
            onClick { onClick() }
        }
    ) {
        content()
    }
}
