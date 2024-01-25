package org.jetbrains.compose.demo.widgets.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.demo.widgets.ui.screens.*

@Composable
fun WidgetsView(
    widgetsTypeState: MutableState<WidgetsType>,
    modifier: Modifier
) {
    ClearFocusBox {
        Box(modifier = modifier.verticalScroll(state = rememberScrollState())) {
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

/**
 * This wrapper need to control focus behavior on iOS to hide the keyboard.
 */
@Composable
private fun ClearFocusBox(content: @Composable () -> Unit) {
    val focusManager = LocalFocusManager.current
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus(force = true)
                }
            },
    ) {
        content()
    }
}
