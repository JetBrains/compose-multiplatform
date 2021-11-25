package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.implementation
import androidx.compose.runtime.Composable
import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import androidx.compose.foundation.layout.Box as JBox

@Composable
@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
internal actual fun BoxActual(modifier: Modifier, content: @Composable () -> Unit) {
    JBox(modifier.implementation) {
        content.invoke()
    }
}
