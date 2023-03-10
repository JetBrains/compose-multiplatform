package example.imageviewer.view

import androidx.compose.ui.Modifier
import example.imageviewer.model.ScalableState

actual fun Modifier.addUserInput(state: ScalableState): Modifier =
    addTouchUserInput(state)
