package org.jetbrains.compose.common.foundation

import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.internal.castOrCreate
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets

@ExperimentalComposeWebWidgets
actual fun Modifier.clickable(onClick: () -> Unit): Modifier = castOrCreate().apply {
    addAttributeBuilder {
        onClick { onClick() }
    }
}
