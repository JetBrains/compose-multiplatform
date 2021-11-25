package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import org.jetbrains.compose.annotations.webWidgetsDeprecationMessage
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.common.ui.Alignment
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.web.ui.Styles

@OptIn(ExperimentalComposeWebWidgetsApi::class)
private fun Arrangement.Horizontal.asClassName() = when (this) {
    Arrangement.End -> Styles.composeWebArrangementHorizontalEnd
    else -> Styles.composeWebArrangementHorizontalStart
}

@OptIn(ExperimentalComposeWebWidgetsApi::class)
private fun Alignment.Vertical.asClassName() = when (this) {
    Alignment.Top -> Styles.composeWebAlignmentVerticalTop
    Alignment.CenterVertically -> Styles.composeWebAlignmentVerticalCenter
    else -> Styles.composeWebAlignmentVerticalBottom
}

@Composable
@ExperimentalComposeWebWidgetsApi
@Deprecated(message = webWidgetsDeprecationMessage)
internal actual fun RowActual(
    modifier: Modifier,
    horizontalArrangement: Arrangement.Horizontal,
    verticalAlignment: Alignment.Vertical,
    content: @Composable () -> Unit
) {
    Div(
        attrs = {
            classes(
                *arrayOf(
                    Styles.rowClass,
                    horizontalArrangement.asClassName(),
                    verticalAlignment.asClassName()
                )
            )
        }
    ) {
        content()
    }
}
