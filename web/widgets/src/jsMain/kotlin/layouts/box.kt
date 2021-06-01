package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.common.ui.asAttributeBuilderApplier

@Composable
internal actual fun BoxActual(modifier: Modifier, content: @Composable () -> Unit) {
    Div(
        attrs = modifier.asAttributeBuilderApplier()
    ) {
        content()
    }
}
