package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable

@Composable
internal expect fun BoxActual(modifier: Modifier, content: @Composable () -> Unit)
