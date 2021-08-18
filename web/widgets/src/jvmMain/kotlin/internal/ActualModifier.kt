package org.jetbrains.compose.common.internal

import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import androidx.compose.ui.Modifier as JModifier
import org.jetbrains.compose.common.ui.Modifier

private class ModifierElement : JModifier.Element

@ExperimentalComposeWebWidgets
class ActualModifier : Modifier {
    var modifier: JModifier = ModifierElement()
}

@ExperimentalComposeWebWidgets
fun Modifier.castOrCreate(): ActualModifier = (this as? ActualModifier) ?: ActualModifier()
