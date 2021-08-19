package org.jetbrains.compose.common.material

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@Composable
@ExperimentalComposeWebWidgetsApi
internal expect fun ButtonActual(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
)
