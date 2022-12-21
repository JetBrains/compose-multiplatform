package example.imageviewer.view

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier

actual fun Modifier.addUserInput(state: MutableState<ScalableState>): Modifier =
    addTouchUserInput(state)
