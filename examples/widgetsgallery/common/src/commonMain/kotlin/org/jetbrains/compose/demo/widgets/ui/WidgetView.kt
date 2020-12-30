package org.jetbrains.compose.demo.widgets.ui

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import org.jetbrains.compose.demo.widgets.ui.screens.*

@Composable
fun WidgetsView(
    widgetsTypeState: MutableState<WidgetsType>,
    modifier: Modifier
) {
    ScrollableColumn(modifier = modifier) {
        Column {
            @Suppress("UNUSED_VARIABLE")
            val exhaustive = when (widgetsTypeState.value) {
                WidgetsType.APP_BARS -> AppBars()
                WidgetsType.BUTTONS -> Buttons()
                WidgetsType.CHIPS -> Chips()
                WidgetsType.LOADERS -> Loaders()
                WidgetsType.SNACK_BARS -> SnackBars()
                WidgetsType.TEXT_VIEWS -> TextViews()
                WidgetsType.TEXT_INPUTS -> TextInputs()
                WidgetsType.TOGGLES -> Toggles()
                WidgetsType.UI_CARDS -> UICards()
            }
        }
    }
}
