package org.jetbrains.compose.common.material

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets

@Composable
@ExperimentalComposeWebWidgets
fun Button(
    modifier: Modifier = Modifier.Companion,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ButtonActual(modifier, onClick, content)
}
