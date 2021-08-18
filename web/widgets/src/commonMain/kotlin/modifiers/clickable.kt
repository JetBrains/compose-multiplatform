package org.jetbrains.compose.common.foundation

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import org.jetbrains.compose.common.ui.Modifier

@ExperimentalComposeWebWidgets
expect fun Modifier.clickable(onClick: () -> Unit): Modifier
