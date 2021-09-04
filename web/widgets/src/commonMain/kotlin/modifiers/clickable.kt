package org.jetbrains.compose.common.foundation

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.common.ui.Modifier

@ExperimentalComposeWebWidgetsApi
expect fun Modifier.clickable(onClick: () -> Unit): Modifier
