package org.jetbrains.compose.common.foundation

import org.jetbrains.compose.common.ui.Modifier

expect fun Modifier.clickable(onClick: () -> Unit): Modifier
