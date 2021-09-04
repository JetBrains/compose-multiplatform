package org.jetbrains.compose.common.internal

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import androidx.compose.ui.Modifier as JModifier
import org.jetbrains.compose.common.ui.Modifier

private class ModifierElement : JModifier.Element

@ExperimentalComposeWebWidgetsApi
class ActualModifier : Modifier {
    var modifier: JModifier = ModifierElement()
}

@ExperimentalComposeWebWidgetsApi
fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
