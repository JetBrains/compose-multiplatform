package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@Composable
@ExperimentalComposeWebWidgetsApi
fun Box(
    modifier: Modifier = Modifier.Companion,
    content: @Composable () -> Unit
) { BoxActual(modifier, content) }
