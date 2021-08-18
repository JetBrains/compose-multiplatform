package org.jetbrains.compose.common.material

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import org.jetbrains.compose.common.ui.implementation
import androidx.compose.material.Button as JButton

@Composable
@ExperimentalComposeWebWidgets
internal actual fun ButtonActual(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    JButton(onClick, modifier.implementation) {
        content()
    }
}
