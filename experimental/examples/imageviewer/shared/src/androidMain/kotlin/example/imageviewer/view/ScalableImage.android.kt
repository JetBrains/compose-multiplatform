package example.imageviewer.view

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import example.imageviewer.model.ScalableState

actual fun Modifier.addUserInput(state: MutableState<ScalableState>) =
    addTouchUserInput(state)
