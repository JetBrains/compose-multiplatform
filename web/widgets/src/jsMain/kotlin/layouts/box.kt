package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.asStyleBuilderApplier
import androidx.compose.runtime.Composable
import androidx.compose.web.elements.Div
import org.jetbrains.compose.common.ui.asAttributeBuilderApplier

@Composable
internal actual fun BoxActual(modifier: Modifier, content: @Composable () -> Unit) {
    Div(
        style = modifier.asStyleBuilderApplier(),
        attrs = modifier.asAttributeBuilderApplier()
    ) {
        content()
    }
}
