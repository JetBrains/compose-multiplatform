package org.jetbrains.compose.common.foundation

import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import androidx.compose.foundation.clickable
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi

@ExperimentalComposeWebWidgetsApi
actual fun Modifier.clickable(onClick: () -> Unit): Modifier = castOrCreate().apply {
    modifier = modifier.clickable(onClick = onClick)
}
